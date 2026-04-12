package com.kheprix.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.kheprix.R
import com.kheprix.api.RetrofitClient
import com.kheprix.api.SessionManager
import com.kheprix.databinding.ActivityUnidadesBinding
import com.kheprix.models.UnidadeResponse
import kotlinx.coroutines.launch

/**
 * Lista de Unidades Amostrais de uma Campanha.
 *
 * Exibe:
 *  - Botão "Adicionar Unidade" → NovaUnidadeActivity
 *  - Botão "Visualizar Detalhes" → toggle card inline com dados da campanha + botão editar
 *  - Lista: nome da unidade + coordenadas formatadas em DMS, lixeira
 *
 * Extras recebidos:
 *   estudo_remote_id  → Int
 *   campanha_id       → Int
 *   campanha_nome     → String
 *   estudo_nome       → String
 */
class UnidadesActivity : AppCompatActivity() {

    private lateinit var binding: ActivityUnidadesBinding
    private var estudoRemoteId = -1
    private var campanhaId     = -1
    private var campanhaNome   = ""
    private var estudoNome     = ""
    private val unidades = mutableListOf<UnidadeResponse>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUnidadesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        estudoRemoteId = intent.getIntExtra("estudo_remote_id", -1)
        campanhaId     = intent.getIntExtra("campanha_id", -1)
        campanhaNome   = intent.getStringExtra("campanha_nome") ?: ""
        estudoNome     = intent.getStringExtra("estudo_nome") ?: ""

        binding.tvCampanhaNome.text = campanhaNome

        val adapter = UnidadeAdapter(unidades,
            onItemClick = { u -> abrirEventos(u) },
            onDeleteClick = { u -> confirmarDelete(u) }
        )
        binding.rvUnidades.layoutManager = LinearLayoutManager(this)
        binding.rvUnidades.adapter = adapter

        binding.btnAdicionarUnidade.setOnClickListener {
            startActivity(Intent(this, NovaUnidadeActivity::class.java).apply {
                putExtra("estudo_remote_id", estudoRemoteId)
                putExtra("campanha_id", campanhaId)
            })
        }

        // "Visualizar Detalhes" → toggle card da campanha
        binding.btnVisualizarDetalhes.setOnClickListener {
            val visivel = binding.cardDetalhesCampanha.visibility == View.VISIBLE
            binding.cardDetalhesCampanha.visibility = if (visivel) View.GONE else View.VISIBLE
        }

        // Botão editar dentro do card de detalhes
        binding.btnEditarCampanha.setOnClickListener {
            startActivity(Intent(this, NovaCampanhaActivityV2::class.java).apply {
                putExtra("estudo_remote_id", estudoRemoteId)
                putExtra("campanha_id", campanhaId)
                putExtra("campanha_nome", campanhaNome)
            })
        }

        binding.ivMenuLateral.setOnClickListener { onBackPressed() }
        binding.ivPerfil.setOnClickListener {
            startActivity(Intent(this, PerfilActivity::class.java))
        }

        carregarDetalhes()
        carregarUnidades()
    }

    override fun onResume() {
        super.onResume()
        carregarUnidades()
    }

    private fun carregarDetalhes() {
        lifecycleScope.launch {
            try {
                val resp = RetrofitClient.apiService.getCampanha(
                    SessionManager.getAuthHeader(), estudoRemoteId, campanhaId
                )
                resp.body()?.let { c ->
                    binding.tvDetalheNome.text      = c.nome
                    binding.tvDetalheInicio.text    = formatarData(c.dataInicio)
                    binding.tvDetalheFim.text       = if (c.dataFim != null) formatarData(c.dataFim) else "—"
                    binding.tvDetalheDescricao.text = c.descricao ?: "—"
                }
            } catch (_: Exception) {}
        }
    }

    private fun carregarUnidades() {
        lifecycleScope.launch {
            try {
                val resp = RetrofitClient.apiService.getUnidades(
                    SessionManager.getAuthHeader(), estudoRemoteId, campanhaId
                )
                if (resp.isSuccessful) {
                    unidades.clear()
                    unidades.addAll(resp.body() ?: emptyList())
                    binding.rvUnidades.adapter?.notifyDataSetChanged()
                }
            } catch (_: Exception) {
                Toast.makeText(this@UnidadesActivity, "Sem conexão", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun abrirEventos(u: UnidadeResponse) {
        startActivity(Intent(this, EventosActivity::class.java).apply {
            putExtra("estudo_remote_id", estudoRemoteId)
            putExtra("campanha_id", campanhaId)
            putExtra("unidade_id", u.id)
            putExtra("unidade_nome", u.nome)
        })
    }

    private fun confirmarDelete(u: UnidadeResponse) {
        AlertDialog.Builder(this)
            .setTitle("Deletar unidade")
            .setMessage("Deletar \"${u.nome}\"?")
            .setPositiveButton("Deletar") { _, _ ->
                lifecycleScope.launch {
                    try {
                        RetrofitClient.apiService.deleteUnidade(
                            SessionManager.getAuthHeader(), estudoRemoteId, campanhaId, u.id
                        )
                        carregarUnidades()
                    } catch (_: Exception) {}
                }
            }
            .setNegativeButton("Cancelar", null).show()
    }

    private fun formatarData(iso: String) = try {
        val p = iso.substring(0, 10).split("-"); "${p[2]}/${p[1]}/${p[0]}"
    } catch (_: Exception) { iso }
}

// ── Adapter ──────────────────────────────────────────────────────────────────

class UnidadeAdapter(
    private val items: List<UnidadeResponse>,
    private val onItemClick: (UnidadeResponse) -> Unit,
    private val onDeleteClick: (UnidadeResponse) -> Unit
) : RecyclerView.Adapter<UnidadeAdapter.VH>() {

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        val tvNome: TextView    = view.findViewById(R.id.tvUnidadeNome)
        val tvCoord: TextView   = view.findViewById(R.id.tvUnidadeCoord)
        val ivDelete: ImageView = view.findViewById(R.id.ivDeleteUnidade)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(LayoutInflater.from(parent.context).inflate(R.layout.item_unidade, parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        holder.tvNome.text  = item.nome
        holder.tvCoord.text = "${decimalToDms(item.latitude)}, ${decimalToDms(item.longitude)}"
        holder.itemView.setOnClickListener { onItemClick(item) }
        holder.ivDelete.setOnClickListener { onDeleteClick(item) }
    }

    override fun getItemCount() = items.size

    /** Converte decimal (−25.43) para DMS "−25°25'48\"" */
    private fun decimalToDms(dec: Double): String {
        val neg  = dec < 0
        val abs  = Math.abs(dec)
        val deg  = abs.toInt()
        val minD = (abs - deg) * 60
        val min  = minD.toInt()
        val sec  = ((minD - min) * 60).toInt()
        return "${if (neg) "-" else ""}${deg}°${min}'${sec}\""
    }
}
