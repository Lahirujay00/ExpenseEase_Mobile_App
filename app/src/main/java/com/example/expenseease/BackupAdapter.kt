package com.example.expenseease

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.expenseease.databinding.ItemBackupBinding
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class BackupAdapter(
    private var backups: List<String>,
    private val onBackupClick: (String) -> Unit
) : RecyclerView.Adapter<BackupAdapter.BackupViewHolder>() {

    inner class BackupViewHolder(private val binding: ItemBackupBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(backupPath: String) {
            val backupFile = File(backupPath)
            val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
            val backupDate = dateFormat.format(Date(backupFile.lastModified()))
            
            binding.tvBackupDate.text = backupDate
            binding.root.setOnClickListener { onBackupClick(backupPath) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BackupViewHolder {
        val binding = ItemBackupBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return BackupViewHolder(binding)
    }

    override fun onBindViewHolder(holder: BackupViewHolder, position: Int) {
        holder.bind(backups[position])
    }

    override fun getItemCount(): Int = backups.size

    fun updateBackups(newBackups: List<String>) {
        backups = newBackups
        notifyDataSetChanged()
    }
} 