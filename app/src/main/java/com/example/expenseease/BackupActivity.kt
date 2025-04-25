package com.example.expenseease

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.expenseease.databinding.ActivityBackupBinding
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class BackupActivity : AppCompatActivity() {
    private lateinit var binding: ActivityBackupBinding
    private lateinit var backupManager: BackupManager
    private lateinit var backupAdapter: BackupAdapter
    private lateinit var transactionManager: TransactionManager

    // File picker for export
    private val exportFileLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val uri = result.data?.data
            if (uri != null) {
                exportDataToUri(uri)
            }
        }
    }

    // File picker for restore
    private val restoreFileLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val uri = result.data?.data
            if (uri != null) {
                restoreFromUri(uri)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBackupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize managers

        backupManager = BackupManager(this)
        transactionManager = TransactionManager(this)

        // Setup toolbar
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Backup & Restore"

        // Setup backup list
        setupBackupList()

        // Setup click listeners
        setupClickListeners()
    }

    private fun setupBackupList() {
        backupAdapter = BackupAdapter(emptyList()) { backupPath ->
            showRestoreDialog(backupPath)
        }

        binding.rvBackups.apply {
            layoutManager = LinearLayoutManager(this@BackupActivity)
            adapter = backupAdapter
        }

        refreshBackupList()
    }

    private fun setupClickListeners() {
        // Create backup button
        binding.btnCreateBackup.setOnClickListener {
            createBackup()
        }

        // Restore from file button
        binding.btnRestoreFromFile.setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "application/json"
            }
            restoreFileLauncher.launch(intent)
        }

        // Export data button
        binding.btnExportData.setOnClickListener {
            showExportOptionsDialog()
        }
    }

    private fun showExportOptionsDialog() {
        val options = arrayOf("Export as JSON", "Export as CSV")
        AlertDialog.Builder(this)
            .setTitle("Export Data")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> exportDataAsJson()
                    1 -> exportDataAsCsv()
                }
            }
            .show()
    }

    private fun exportDataAsJson() {
        binding.progressBar.visibility = View.VISIBLE
        binding.btnExportData.isEnabled = false

        Thread {
            try {
                // Get all transactions
                val transactions = transactionManager.getAllTransactions()
                
                // Create a JSON object
                val gson = GsonBuilder().setPrettyPrinting().create()
                val jsonData = gson.toJson(transactions)
                
                // Create a temporary file
                val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                val fileName = "expenseease_transactions_$timestamp.json"
                val file = File(cacheDir, fileName)
                
                // Write JSON to file
                FileOutputStream(file).use { 
                    it.write(jsonData.toByteArray()) 
                }
                
                // Share the file
                runOnUiThread {
                    shareFile(file, "application/json", fileName)
                }
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this, "Error exporting data: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            } finally {
                runOnUiThread {
                    binding.progressBar.visibility = View.GONE
                    binding.btnExportData.isEnabled = true
                }
            }
        }.start()
    }

    private fun exportDataAsCsv() {
        binding.progressBar.visibility = View.VISIBLE
        binding.btnExportData.isEnabled = false

        Thread {
            try {
                // Get all transactions
                val transactions = transactionManager.getAllTransactions()
                
                // Create CSV header
                val csvBuilder = StringBuilder()
                csvBuilder.append("ID,Title,Amount,Category,Description,Date,Is Income\n")
                
                // Add transaction data
                val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                for (transaction in transactions) {
                    csvBuilder.append("${transaction.id},")
                    csvBuilder.append("\"${transaction.title.replace("\"", "\"\"")}\",")
                    csvBuilder.append("${transaction.amount},")
                    csvBuilder.append("\"${transaction.category.replace("\"", "\"\"")}\",")
                    csvBuilder.append("\"${transaction.description.replace("\"", "\"\"")}\",")
                    csvBuilder.append("\"${dateFormat.format(Date(transaction.timestamp))}\",")
                    csvBuilder.append("${transaction.isIncome}\n")
                }
                
                // Create a temporary file
                val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                val fileName = "expenseease_transactions_$timestamp.csv"
                val file = File(cacheDir, fileName)
                
                // Write CSV to file
                FileOutputStream(file).use { 
                    it.write(csvBuilder.toString().toByteArray()) 
                }
                
                // Share the file
                runOnUiThread {
                    shareFile(file, "text/csv", fileName)
                }
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this, "Error exporting data: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            } finally {
                runOnUiThread {
                    binding.progressBar.visibility = View.GONE
                    binding.btnExportData.isEnabled = true
                }
            }
        }.start()
    }

    private fun shareFile(file: File, mimeType: String, fileName: String) {
        try {
            // Create a content URI for the file using FileProvider
            val fileUri = FileProvider.getUriForFile(
                this,
                "${packageName}.fileprovider",
                file
            )
            
            // Create an intent to share the file
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, fileUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            
            // Start the share activity
            startActivity(Intent.createChooser(shareIntent, "Share $fileName"))
            
            Toast.makeText(this, "Data exported successfully", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Error sharing file: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun exportDataToUri(uri: Uri) {
        binding.progressBar.visibility = View.VISIBLE
        binding.btnExportData.isEnabled = false

        Thread {
            try {
                // Get all transactions
                val transactions = transactionManager.getAllTransactions()
                
                // Create a JSON object
                val gson = GsonBuilder().setPrettyPrinting().create()
                val jsonData = gson.toJson(transactions)
                
                // Write to the selected URI
                contentResolver.openOutputStream(uri)?.use { outputStream ->
                    outputStream.write(jsonData.toByteArray())
                }
                
                runOnUiThread {
                    Toast.makeText(this, "Data exported successfully", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this, "Error exporting data: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            } finally {
                runOnUiThread {
                    binding.progressBar.visibility = View.GONE
                    binding.btnExportData.isEnabled = true
                }
            }
        }.start()
    }

    private fun createBackup() {
        binding.progressBar.visibility = View.VISIBLE
        binding.btnCreateBackup.isEnabled = false
        binding.btnRestoreFromFile.isEnabled = false
        binding.btnExportData.isEnabled = false

        // Create backup in background thread
        Thread {
            try {
                val backupPath = backupManager.createBackup()
                
                runOnUiThread {
                    binding.progressBar.visibility = View.GONE
                    binding.btnCreateBackup.isEnabled = true
                    binding.btnRestoreFromFile.isEnabled = true
                    binding.btnExportData.isEnabled = true
                    
                    if (backupPath != null) {
                        Toast.makeText(this, "Backup created successfully at: $backupPath", Toast.LENGTH_LONG).show()
                        refreshBackupList()
                    } else {
                        Toast.makeText(this, "Failed to create backup. Check logs for details.", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                runOnUiThread {
                    binding.progressBar.visibility = View.GONE
                    binding.btnCreateBackup.isEnabled = true
                    binding.btnRestoreFromFile.isEnabled = true
                    binding.btnExportData.isEnabled = true
                    Toast.makeText(this, "Error creating backup: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }.start()
    }

    private fun showRestoreDialog(backupPath: String) {
        val backupFile = File(backupPath)
        val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
        val backupDate = dateFormat.format(Date(backupFile.lastModified()))

        AlertDialog.Builder(this)
            .setTitle("Restore Backup")
            .setMessage("Are you sure you want to restore the backup from $backupDate? This will replace all current data.")
            .setPositiveButton("Restore") { _, _ ->
                restoreBackup(backupPath)
            }
            .setNegativeButton("Cancel", null)
            .setNeutralButton("Delete Backup") { _, _ ->
                showDeleteDialog(backupPath)
            }
            .show()
    }

    private fun showDeleteDialog(backupPath: String) {
        AlertDialog.Builder(this)
            .setTitle("Delete Backup")
            .setMessage("Are you sure you want to delete this backup? This action cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                deleteBackup(backupPath)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun restoreBackup(backupPath: String) {
        binding.progressBar.visibility = View.VISIBLE
        binding.btnCreateBackup.isEnabled = false
        binding.btnRestoreFromFile.isEnabled = false
        binding.btnExportData.isEnabled = false

        // Restore backup in background thread
        Thread {
            val success = backupManager.restoreBackup(backupPath)
            
            runOnUiThread {
                binding.progressBar.visibility = View.GONE
                binding.btnCreateBackup.isEnabled = true
                binding.btnRestoreFromFile.isEnabled = true
                binding.btnExportData.isEnabled = true
                
                if (success) {
                    Toast.makeText(this, "Backup restored successfully", Toast.LENGTH_SHORT).show()
                    // Return to home activity
                    finish()
                } else {
                    Toast.makeText(this, "Failed to restore backup", Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }

    private fun deleteBackup(backupPath: String) {
        val success = backupManager.deleteBackup(backupPath)
        if (success) {
            Toast.makeText(this, "Backup deleted successfully", Toast.LENGTH_SHORT).show()
            refreshBackupList()
        } else {
            Toast.makeText(this, "Failed to delete backup", Toast.LENGTH_SHORT).show()
        }
    }

    private fun refreshBackupList() {
        Log.d("BackupActivity", "Refreshing backup list...")
        val backups = backupManager.getAvailableBackups()
        Log.d("BackupActivity", "Found ${backups.size} backups")
        
        backupAdapter.updateBackups(backups)
        
        // Show/hide empty state
        if (backups.isEmpty()) {
            Log.d("BackupActivity", "No backups found, showing empty state")
            binding.tvNoBackups.visibility = View.VISIBLE
            binding.rvBackups.visibility = View.GONE
        } else {
            Log.d("BackupActivity", "Showing backup list")
            binding.tvNoBackups.visibility = View.GONE
            binding.rvBackups.visibility = View.VISIBLE
        }
    }

    private fun restoreFromUri(uri: Uri) {
        binding.progressBar.visibility = View.VISIBLE
        binding.btnCreateBackup.isEnabled = false
        binding.btnRestoreFromFile.isEnabled = false
        binding.btnExportData.isEnabled = false

        Thread {
            try {
                // Read the JSON data from the URI
                val jsonData = contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                    ?: throw Exception("Failed to read backup file")

                // Create a temporary file to store the backup
                val backupFile = File(cacheDir, "temp_backup.json")
                FileOutputStream(backupFile).use { 
                    it.write(jsonData.toByteArray()) 
                }

                // Restore from the temporary file
                val success = backupManager.restoreBackup(backupFile.absolutePath)

                runOnUiThread {
                    binding.progressBar.visibility = View.GONE
                    binding.btnCreateBackup.isEnabled = true
                    binding.btnRestoreFromFile.isEnabled = true
                    binding.btnExportData.isEnabled = true

                    if (success) {
                        Toast.makeText(this, "Backup restored successfully", Toast.LENGTH_SHORT).show()
                        // Return to home activity
                        finish()
                    } else {
                        Toast.makeText(this, "Failed to restore backup", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                runOnUiThread {
                    binding.progressBar.visibility = View.GONE
                    binding.btnCreateBackup.isEnabled = true
                    binding.btnRestoreFromFile.isEnabled = true
                    binding.btnExportData.isEnabled = true
                    Toast.makeText(this, "Error restoring backup: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
} 