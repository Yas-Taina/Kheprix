package com.kheprix.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.kheprix.api.SessionManager
import com.kheprix.databinding.ActivityPerfilBinding
import com.kheprix.db.EstudoOfflineManager

class PerfilActivity : BaseDrawerActivity() {

    private lateinit var binding: ActivityPerfilBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPerfilBinding.inflate(layoutInflater)
        setContentView(binding.root)

        exibirDadosUsuario()
        exibirResumoOffline()

        binding.ivMenuLateral.setOnClickListener { openDrawer() }

        binding.btnLogout.setOnClickListener {
            confirmarLogout()
        }
    }

    private fun exibirDadosUsuario() {
        val nome  = SessionManager.getUserName()
        val email = SessionManager.getUserEmail()

        binding.tvNomeUsuario.text  = nome  ?: "—"
        binding.tvEmailUsuario.text = email ?: "—"
    }

    private fun exibirResumoOffline() {
        try {
            val manager = EstudoOfflineManager(this)
            val db = com.kheprix.db.DatabaseHelper(this).readableDatabase
            val cursor = db.rawQuery(
                "SELECT local_id FROM estudos", null
            )
            var totalOffline = 0
            var totalEstudos = 0
            cursor.use { c ->
                while (c.moveToNext()) {
                    val localId = c.getLong(0)
                    totalOffline += manager.contarRegistrosOffline(localId)
                    totalEstudos++
                }
            }
            binding.tvEstudosLocais.text = "$totalEstudos estudo(s) salvo(s) localmente"
            binding.tvRegistrosOffline.text = "$totalOffline registro(s) não sincronizado(s)"
            binding.tvRegistrosOffline.visibility =
                if (totalOffline > 0) View.VISIBLE else View.GONE
        } catch (_: Exception) {
            binding.tvEstudosLocais.text   = "—"
            binding.tvRegistrosOffline.visibility = View.GONE
        }
    }

    private fun confirmarLogout() {
        AlertDialog.Builder(this)
            .setTitle("Sair da conta")
            .setMessage("Tem certeza que deseja sair? Os dados salvos offline permanecerão no dispositivo.")
            .setPositiveButton("Sair") { _, _ -> realizarLogout() }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun realizarLogout() {
        SessionManager.logout()
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
    }
}
