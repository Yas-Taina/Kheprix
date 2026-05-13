package com.kheprix.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.core.view.GravityCompat
import androidx.lifecycle.lifecycleScope
import com.kheprix.R
import com.kheprix.api.RetrofitClient
import com.kheprix.api.SessionManager
import com.kheprix.databinding.ActivityHomeBinding
import com.kheprix.db.DatabaseHelper
import com.kheprix.db.EstudoOfflineManager
import kotlinx.coroutines.launch

class HomeActivity : BaseDrawerActivity() {

    private lateinit var binding: ActivityHomeBinding
    private lateinit var offlineManager: EstudoOfflineManager

    private var ultimoEstudoRemoteId = -1
    private var ultimoEstudoLocalId = -1L
    private var popupOfflineVisivel = false

    override fun onEstadoOffline(offline: Boolean) {
        if (offline && !popupOfflineVisivel && !isFinishing) {
            popupOfflineVisivel = true
            android.app.AlertDialog.Builder(this)
                .setTitle("Você está offline")
                .setMessage("Sem conexão com a internet. Você pode continuar usando o app, mas algumas funcionalidades estarão indisponíveis.")
                .setPositiveButton("Entendi") { _, _ -> popupOfflineVisivel = false }
                .show()
        } else if (!offline) {
            popupOfflineVisivel = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupDrawer(binding.drawerLayout, binding.navigationView)

        offlineManager = EstudoOfflineManager(this)
        setupCardListeners()
    }

    override fun onResume() {
        super.onResume()
        carregarUltimoEstudo()
    }

    private fun setupCardListeners() {
        binding.ivMenuLateral.setOnClickListener {
            binding.drawerLayout.openDrawer(GravityCompat.START)
        }
        binding.ivPerfil.setOnClickListener {
            startActivity(Intent(this, PerfilActivity::class.java))
        }

        binding.cardRegistroRapido.setOnClickListener {
            startActivity(Intent(this, RegistroRapidoActivity::class.java))
        }
        binding.cardVisualizarEstudos.setOnClickListener {
            startActivity(Intent(this, EstudosActivity::class.java))
        }
        binding.cardNovoEstudo.setOnClickListener {
            startActivity(Intent(this, NovoEstudoActivity::class.java))
        }
        binding.cardConvites.setOnClickListener {
            startActivity(Intent(this, ConvitesActivity::class.java))
        }
        binding.btnVisualizarUltimo.setOnClickListener {
            if (ultimoEstudoRemoteId == -1 && ultimoEstudoLocalId == -1L) return@setOnClickListener
            startActivity(Intent(this, EstudoDetalheActivity::class.java).apply {
                putExtra(EstudoDetalheActivity.EXTRA_ESTUDO_REMOTE_ID, ultimoEstudoRemoteId)
                putExtra(EstudoDetalheActivity.EXTRA_ESTUDO_LOCAL_ID, ultimoEstudoLocalId)
                putExtra(
                    EstudoDetalheActivity.EXTRA_ESTUDO_NOME,
                    binding.tvUltimoEstudoNome.text.toString()
                )
            })
        }
        binding.cardUltimoEstudo.setOnClickListener { binding.btnVisualizarUltimo.performClick() }
    }

    private fun carregarUltimoEstudo() {
        lifecycleScope.launch {
            try {
                val resp = RetrofitClient.apiService.getDashboard(SessionManager.getAuthHeader())
                when {
                    resp.isSuccessful -> {
                        val dash = resp.body()
                        if (dash == null) {
                            binding.cardUltimoEstudo.visibility = View.GONE; return@launch
                        }
                        ultimoEstudoRemoteId = dash.id
                        ultimoEstudoLocalId = buscarLocalId(dash.id)
                        binding.tvUltimoEstudoNome.text = dash.nome
                        binding.tvUltimoEstudoData.text =
                            "Editado em ${formatarData(dash.updatedAt)}"
                        val c =
                            if (ultimoEstudoLocalId != -1L) offlineManager.contarRegistrosOffline(
                                ultimoEstudoLocalId
                            ) else 0
                        binding.tvOfflineCount.text = "$c Registros Offline"
                        binding.tvOfflineCount.visibility = if (c > 0) View.VISIBLE else View.GONE
                        binding.cardUltimoEstudo.visibility = View.VISIBLE
                    }

                    resp.code() == 404 -> binding.cardUltimoEstudo.visibility = View.GONE
                    else -> carregarUltimoEstudoLocal()
                }
            } catch (_: Exception) {
                binding.cardUltimoEstudo.visibility = View.GONE
            }
        }
    }

    private fun carregarUltimoEstudoLocal() {
        val db = DatabaseHelper(this).readableDatabase
        db.rawQuery(
            "SELECT local_id, remote_id, nome, updated_at FROM estudos ORDER BY updated_at DESC LIMIT 1",
            null
        )
            .use { cur ->
                if (cur.moveToFirst()) {
                    ultimoEstudoLocalId = cur.getLong(0)
                    ultimoEstudoRemoteId = if (cur.isNull(1)) -1 else cur.getInt(1)
                    binding.tvUltimoEstudoNome.text = cur.getString(2) ?: "—"
                    binding.tvUltimoEstudoData.text =
                        "Editado em ${formatarData(cur.getString(3) ?: "")}"
                    val c = offlineManager.contarRegistrosOffline(ultimoEstudoLocalId)
                    binding.tvOfflineCount.text = "$c Registros Offline"
                    binding.tvOfflineCount.visibility = if (c > 0) View.VISIBLE else View.GONE
                    binding.cardUltimoEstudo.visibility = View.VISIBLE
                } else binding.cardUltimoEstudo.visibility = View.GONE
            }
    }

    private fun buscarLocalId(remoteId: Int): Long {
        val db = DatabaseHelper(this).readableDatabase
        return db.rawQuery(
            "SELECT local_id FROM estudos WHERE remote_id = ?",
            arrayOf(remoteId.toString())
        )
            .use { if (it.moveToFirst()) it.getLong(0) else -1L }
    }

    private fun formatarData(iso: String) = try {
        val p = iso.substring(0, 10).split("-"); "${p[2]}/${p[1]}/${p[0]}"
    } catch (_: Exception) {
        iso
    }
}
