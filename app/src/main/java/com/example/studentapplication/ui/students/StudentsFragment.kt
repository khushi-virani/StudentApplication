package com.example.studentapplication.ui.students

import android.app.AlertDialog
import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.studentapplication.R
import com.example.studentapplication.data.repository.FirestoreRepository
import com.example.studentapplication.databinding.FragmentStudentsBinding
import com.example.studentapplication.utils.AnalyticsHelper
import com.example.studentapplication.utils.AuthPreferences
import com.example.studentapplication.utils.ExportUtils
import com.example.studentapplication.viewmodel.SortOption
import com.example.studentapplication.viewmodel.StudentViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class StudentsFragment : Fragment(R.layout.fragment_students) {

    private var _binding: FragmentStudentsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: StudentViewModel by viewModels()

    @Inject lateinit var firebaseRepository: FirestoreRepository
    private lateinit var adapter: StudentAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        _binding = FragmentStudentsBinding.bind(view)
        val userId = AuthPreferences(requireContext()).getCurrentUserId()
        viewModel.setUserId(userId)

        adapter = StudentAdapter(emptyList()) { student ->  //runs when row is clicked
            val bundle = Bundle().apply { putInt("studentId", student.id) }
            findNavController().navigate(R.id.studentDetailFragment, bundle)
        }

        AnalyticsHelper.logScreenView("StudentScreen")

        binding.rvStudents.layoutManager = LinearLayoutManager(requireContext())
        binding.rvStudents.adapter = adapter

        setupSwipeToDelete()
        setupExportMenu()       //  attach export menu
        binding.chipAll.post {
            highlightChip(binding.chipAll)
        }

        // Load default list
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.students.collect { students ->
                    adapter.updateList(students)
                    if (viewModel.isUserIdSet()) {
                        showEmptyState(students.isEmpty())
                    }
                }
            }
        }

        // Check incoming search query from Home
        val incomingQuery = arguments?.getString("searchQuery") ?: ""
        if (incomingQuery.isNotEmpty()) {
            binding.etSearch.setText(incomingQuery)
            viewModel.setSearchQuery(incomingQuery)
        }

        //  Search — just update StateFlow
        binding.etSearch.addTextChangedListener { editable ->
            val query = editable?.toString()?.trim() ?: ""
            viewModel.setSearchQuery(query)
            if(query.length >= 3){
                AnalyticsHelper.logSearch(query)
            }
        }

        // Sort chips
        binding.chipAll.setOnClickListener {
            resetChips()
            highlightChip(binding.chipAll)
            viewModel.setSortOption(SortOption.DEFAULT)
        }
        binding.chipName.setOnClickListener {
            resetChips()
            highlightChip(binding.chipName)
            viewModel.setSortOption(SortOption.NAME)
        }
        binding.chipCourse.setOnClickListener {
            resetChips()
            highlightChip(binding.chipCourse)
            viewModel.setSortOption(SortOption.COURSE)
        }
        binding.chipDateAsc.setOnClickListener {
            resetChips()
            highlightChip(binding.chipDateAsc)
            viewModel.setSortOption(SortOption.DATE_ASC)
        }
        binding.chipDateDesc.setOnClickListener {
            resetChips()
            highlightChip(binding.chipDateDesc)
            viewModel.setSortOption(SortOption.DATE_DESC)
        }

        binding.btnAddFirstStudent.setOnClickListener {
            findNavController().navigate(R.id.addStudentFragment)
        }
    }
    // In StudentsFragment — example usage
//    private fun filterByCourse(course: String) {
//        lifecycleScope.launch {
//            val filtered = firebaseRepository.getStudentsByCourse(course)
//            // show filtered list in your adapter
//            adapter.submitList(filtered)
//            Log.d("test",filtered)
//        }
//    }

    // ─── Export ───────────────────────────────────────────────────────────────

    private fun setupExportMenu() {
        // Manually set up toolbar with export icon — no ActionBar needed
        binding.toolbar.inflateMenu(R.menu.menu_students)
        binding.toolbar.setOnMenuItemClickListener { menuItem ->
            if (menuItem.itemId == R.id.action_export) {
                showExportDialog()
                true
            } else false
        }
    }


    private fun showExportDialog() {
        val students = viewModel.students.value

        // Guard — nothing to export
        if (students.isEmpty()) {
            Toast.makeText(
                requireContext(), "No students to export", android.widget.Toast.LENGTH_SHORT
            ).show()
            return
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Export Student List")
            .setMessage("Choose export format:")
            .setPositiveButton("PDF") { _, _ -> exportAs("pdf") }
            .setNegativeButton("CSV") { _, _ -> exportAs("csv") }
            .setNeutralButton("Cancel", null)
            .show()
    }

    private fun exportAs(format: String) {
        val students = viewModel.students.value

        try {
            // Generate file on IO — then share
            val file = when (format) {
                "pdf" -> ExportUtils.exportToPdf(requireContext(), students)
                else  -> ExportUtils.exportToCsv(requireContext(), students)
            }

            // Create a content URI via FileProvider so other apps can read the file
            val uri = FileProvider.getUriForFile(
                requireContext(),
                "${requireContext().packageName}.fileprovider",
                file
            )

            // Open system share sheet — user can save to Drive, send via email, etc.
            val intent = Intent(Intent.ACTION_SEND).apply {
                type    = if (format == "pdf") "application/pdf" else "text/csv"
                putExtra(Intent.EXTRA_STREAM, uri)  //EXTRA_STREAM -> attach file
                putExtra(Intent.EXTRA_SUBJECT, "Student List Export")//EXTRA_SUBJECT -> subject text
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            startActivity(Intent.createChooser(intent, "Share Student List via"))

        } catch (e: Exception) {
            Toast.makeText(
                requireContext(), "Export failed: ${e.message}", Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun highlightChip(chip: com.google.android.material.chip.Chip) {
        chip.chipBackgroundColor = ColorStateList.valueOf(
            android.graphics.Color.parseColor("#FF9800")
        )
        chip.setTextColor(android.graphics.Color.WHITE)
    }

    private fun resetChips() {
        listOf(
            binding.chipAll,
            binding.chipName,
            binding.chipCourse,
            binding.chipDateAsc,
            binding.chipDateDesc
        ).forEach { chip ->
            chip.chipBackgroundColor = ColorStateList.valueOf(
                ContextCompat.getColor(requireContext(), R.color.chipDefault)
            )
            chip.setTextColor(
                ContextCompat.getColor(requireContext(), R.color.txtColor)
            )
        }
    }

    private fun showEmptyState(isEmpty: Boolean) {
        binding.layoutEmpty.visibility = if (isEmpty) View.VISIBLE else View.GONE
        binding.rvStudents.visibility  = if (isEmpty) View.GONE   else View.VISIBLE
    }

    private fun setupSwipeToDelete() {
        val itemTouchHelper = androidx.recyclerview.widget.ItemTouchHelper(
            object : androidx.recyclerview.widget.ItemTouchHelper.SimpleCallback(
                0,
                androidx.recyclerview.widget.ItemTouchHelper.LEFT or
                        androidx.recyclerview.widget.ItemTouchHelper.RIGHT
            ) {
                override fun onMove(
                    recyclerView: androidx.recyclerview.widget.RecyclerView,
                    viewHolder: androidx.recyclerview.widget.RecyclerView.ViewHolder,
                    target: androidx.recyclerview.widget.RecyclerView.ViewHolder
                ) = false

                override fun onSwiped(
                    viewHolder: androidx.recyclerview.widget.RecyclerView.ViewHolder,
                    direction: Int
                ) {
                    val position = viewHolder.adapterPosition
                    val student  = adapter.getStudentAt(position)

                    viewModel.deleteStudent(student)

                    com.google.android.material.snackbar.Snackbar.make(
                        binding.root,
                        "${student.name} deleted",
                        com.google.android.material.snackbar.Snackbar.LENGTH_LONG
                    ).setAction("UNDO") {
                        viewModel.insertStudent(student)
                    }.setActionTextColor(
                        android.graphics.Color.parseColor("#FF9800")
                    ).show()
                }
            }
        )
        itemTouchHelper.attachToRecyclerView(binding.rvStudents)    //connect swipe behavior to the recyclerview
    }

    override fun onDestroyView() {  //called when fragment view is removed
        super.onDestroyView()
        _binding = null
    }
}
