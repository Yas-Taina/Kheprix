package com.kheprix.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import com.kheprix.api.RetrofitClient
import com.kheprix.api.SessionManager
import com.kheprix.databinding.ActivityPerfilBinding
import com.kheprix.db.EstudoDao
import com.kheprix.db.EstudoOfflineManager
import kotlinx.coroutines.launch

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
        val nome = SessionManager.getUserName()
        val email = SessionManager.getUserEmail()

        binding.tvNomeUsuario.text = nome ?: "—"
        binding.tvEmailUsuario.text = email ?: "—"
    }

    private fun exibirResumoOffline() {
        lifecycleScope.launch {
            val manager = EstudoOfflineManager(this@PerfilActivity)
            val dao = EstudoDao(this@PerfilActivity)

            try {
                val token = SessionManager.getAuthHeader()
                val response = RetrofitClient.apiService.getEstudos(token)

                if (response.isSuccessful) {
                    val apiEstudos = response.body() ?: emptyList()
                    val remoteIds = apiEstudos.map { it.id }.toSet()
                    val estudosOnline = apiEstudos.size

                    val locais = dao.listarTodos()

                    val estudosApenasLocal = locais.count { local ->
                        (local.remoteId == null || local.remoteId !in remoteIds) &&
                                manager.isExplicitamenteSalvoOffline(local.localId)
                    }

                    // Registros offline de estudos online
                    var totalOffline = apiEstudos.sumOf { estudo ->
                        dao.buscarPorRemoteId(estudo.id)?.let { local ->
                            manager.contarRegistrosOffline(local.localId)
                        } ?: 0
                    }
                    // Registros offline de estudos só locais
                    locais.forEach { local ->
                        if ((local.remoteId == null || local.remoteId !in remoteIds) &&
                            manager.isExplicitamenteSalvoOffline(local.localId)
                        ) {
                            totalOffline += manager.contarRegistrosOffline(local.localId)
                        }
                    }

                    mostrarResumo(online = true, estudosOnline, estudosApenasLocal, totalOffline)
                    return@launch
                }
            } catch (_: Exception) {
            }

            var totalOffline = 0
            var estudosApenasLocal = 0
            dao.listarTodos().forEach { local ->
                if (manager.isExplicitamenteSalvoOffline(local.localId)) {
                    estudosApenasLocal++
                    totalOffline += manager.contarRegistrosOffline(local.localId)
                }
            }
            mostrarResumo(online = false, estudosOnline = 0, estudosApenasLocal, totalOffline)
        }
    }

    private fun mostrarResumo(
        online: Boolean,
        estudosOnline: Int,
        estudosApenasLocal: Int,
        totalOffline: Int
    ) {
        binding.tvEstudosLocais.text = when {
            !online && estudosApenasLocal > 0 ->
                "$estudosApenasLocal estudo(s) salvo(s) localmente"

            !online ->
                "sem conexão"

            estudosApenasLocal > 0 && estudosOnline > 0 ->
                "$estudosOnline estudo(s) online · $estudosApenasLocal salvo(s) localmente"

            estudosApenasLocal > 0 ->
                "$estudosApenasLocal estudo(s) salvo(s) localmente"

            else ->
                "$estudosOnline estudo(s) online"
        }
        binding.tvRegistrosOffline.text = "$totalOffline registro(s) não sincronizado(s)"
        binding.tvRegistrosOffline.visibility =
            if (totalOffline > 0) View.VISIBLE else View.GONE
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
