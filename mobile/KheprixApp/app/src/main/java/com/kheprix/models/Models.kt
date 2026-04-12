package com.kheprix.models

import com.google.gson.annotations.SerializedName

// ──────────────────────────────────────────────
// AUTENTICAÇÃO
// ──────────────────────────────────────────────

data class LoginRequest(
    val email: String,
    val senha: String
)

data class LoginResponse(
    val token: String
)

data class EmailRequest(
    val email: String
)

data class TokenRequest(
    val token: String
)

data class ValidoResponse(
    val valido: Boolean
)

data class RedefinirSenhaRequest(
    val token: String,
    @SerializedName("nova_senha") val novaSenha: String
)

data class MensagemResponse(
    val mensagem: String
)

// ──────────────────────────────────────────────
// USUÁRIOS
// ──────────────────────────────────────────────

data class AutocadastroRequest(
    val nome: String,
    val email: String,
    val senha: String
)

data class UsuarioResponse(
    val id: Int,
    val nome: String,
    val email: String,
    @SerializedName("created_at") val createdAt: String
)

// ──────────────────────────────────────────────
// ESTUDOS
// ──────────────────────────────────────────────

data class EstudoRequest(
    val nome: String,
    val observacoes: String? = null,
    val variaveis: List<VariavelRequest>
)

data class EstudoResponse(
    val id: Int,
    val nome: String,
    val observacoes: String?,
    val perfil: String?,
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("updated_at") val updatedAt: String
)

// ──────────────────────────────────────────────
// VARIÁVEIS
// ──────────────────────────────────────────────

data class VariavelRequest(
    val nome: String,
    @SerializedName("nivel_aplicacao") val nivelAplicacao: String,
    @SerializedName("tipo_dado") val tipoDado: String,
    val metrica: String? = null
)

data class VariavelResponse(
    val id: Int,
    val nome: String,
    val metrica: String?,
    @SerializedName("nivel_aplicacao") val nivelAplicacao: String,
    @SerializedName("tipo_dado") val tipoDado: String,
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("updated_at") val updatedAt: String
)

// ──────────────────────────────────────────────
// ESPÉCIES
// ──────────────────────────────────────────────

data class EspecieRequest(
    val classe: String,
    val ordem: String,
    val familia: String,
    val genero: String,
    val especie: String,
    val endemismo: Boolean,
    /** Base64-encoded image */
    val foto: String? = null,
    @SerializedName("nome_popular") val nomePopular: String? = null,
    @SerializedName("status_conservacao") val statusConservacao: String? = null
)

data class EspeciePatchRequest(
    val classe: String? = null,
    val ordem: String? = null,
    val familia: String? = null,
    val genero: String? = null,
    val especie: String? = null,
    val endemismo: Boolean? = null,
    /** Base64-encoded image */
    val foto: String? = null,
    @SerializedName("nome_popular") val nomePopular: String? = null,
    @SerializedName("status_conservacao") val statusConservacao: String? = null
)

data class EspecieResponse(
    val id: Int,
    @SerializedName("estudo_id") val estudoId: Int,
    /** Base64-encoded image */
    val foto: String?,
    val classe: String,
    val ordem: String,
    val familia: String,
    val genero: String,
    val especie: String,
    @SerializedName("nome_popular") val nomePopular: String?,
    @SerializedName("status_conservacao") val statusConservacao: String?,
    val endemismo: Boolean,
    @SerializedName("created_at") val createdAt: String
)

// ──────────────────────────────────────────────
// COLABORADORES
// ──────────────────────────────────────────────

data class ColaboradorResponse(
    @SerializedName("id_usuario") val idUsuario: Int,
    val nome: String,
    val email: String,
    val perfil: String
)

data class ColaboradorPatchRequest(
    val perfil: String
)

// ──────────────────────────────────────────────
// CONVITES
// ──────────────────────────────────────────────

data class ConviteRequest(
    @SerializedName("email_convidado") val emailConvidado: String
)

data class ConviteResponse(
    val id: Int,
    @SerializedName("estudo_id") val estudoId: Int,
    @SerializedName("email_convidado") val emailConvidado: String,
    val token: String,
    val status: String,
    @SerializedName("data_expiracao") val dataExpiracao: String,
    @SerializedName("created_at") val createdAt: String
)

data class ConviteListResponse(
    val id: Int,
    @SerializedName("email_convidado") val emailConvidado: String,
    val status: String,
    @SerializedName("data_expiracao") val dataExpiracao: String,
    @SerializedName("created_at") val createdAt: String
)

data class ConviteRecebidoResponse(
    val id: Int,
    @SerializedName("estudo_id") val estudoId: Int,
    @SerializedName("nome_estudo") val nomeEstudo: String,
    @SerializedName("nome_remetente") val nomeRemetente: String,
    val status: String,
    @SerializedName("data_expiracao") val dataExpiracao: String,
    @SerializedName("created_at") val createdAt: String
)

data class ConviteDetalheResponse(
    val id: Int,
    @SerializedName("estudo_id") val estudoId: Int,
    @SerializedName("nome_estudo") val nomeEstudo: String,
    @SerializedName("email_convidado") val emailConvidado: String,
    val status: String,
    @SerializedName("data_expiracao") val dataExpiracao: String
)

// ──────────────────────────────────────────────
// CÓDIGO DE ACESSO
// ──────────────────────────────────────────────

data class CodigoAcessoResponse(
    val codigo: String,
    @SerializedName("senha_autocadastro") val senhaAutocadastro: String
)

data class CodigoAcessoPatchRequest(
    @SerializedName("senha_autocadastro") val senhaAutocadastro: String
)

// ──────────────────────────────────────────────
// AUTOCADASTRO EM ESTUDO
// ──────────────────────────────────────────────

data class IngressarRequest(
    val codigo: String,
    @SerializedName("senha_autocadastro") val senhaAutocadastro: String
)

data class IngressarResponse(
    @SerializedName("estudo_id") val estudoId: Int,
    @SerializedName("nome_estudo") val nomeEstudo: String,
    val perfil: String
)

// ──────────────────────────────────────────────
// CAMPANHAS
// ──────────────────────────────────────────────

data class CampanhaRequest(
    val nome: String,
    @SerializedName("data_inicio") val dataInicio: String,
    @SerializedName("data_fim") val dataFim: String? = null,
    val descricao: String? = null,
    @SerializedName("valores_variaveis") val valoresVariaveis: List<ValorVariavelRequest>? = null
)

data class ValorVariavelRequest(
    @SerializedName("variavel_id") val variavelId: Int,
    val valor: String
)

data class CampanhaResponse(
    val id: Int,
    val nome: String,
    @SerializedName("data_inicio") val dataInicio: String,
    @SerializedName("data_fim") val dataFim: String?,
    val descricao: String?,
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("updated_at") val updatedAt: String
)

// ──────────────────────────────────────────────
// UNIDADES AMOSTRAIS
// ──────────────────────────────────────────────

data class UnidadeRequest(
    val nome: String,
    val latitude: Double,
    val longitude: Double,
    val raio: Double? = null,
    @SerializedName("metodo_coleta") val metodoColeta: String? = null,
    @SerializedName("esforco_amostral") val esforcoAmostral: String? = null
)

data class UnidadeResponse(
    val id: Int,
    @SerializedName("campanha_id") val campanhaId: Int,
    val nome: String,
    val latitude: Double,
    val longitude: Double,
    val raio: Double?,
    @SerializedName("metodo_coleta") val metodoColeta: String?,
    @SerializedName("esforco_amostral") val esforcoAmostral: String?,
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("updated_at") val updatedAt: String
)

// ──────────────────────────────────────────────
// EVENTOS DE AMOSTRAGEM
// ──────────────────────────────────────────────

data class EventoRequest(
    @SerializedName("horario_inicio") val horarioInicio: String,
    @SerializedName("horario_fim") val horarioFim: String? = null,
    @SerializedName("esforco_real") val esforcoReal: String? = null
)

data class EventoResponse(
    val id: Int,
    @SerializedName("unidade_amostral_id") val unidadeAmostralId: Int,
    @SerializedName("horario_inicio") val horarioInicio: String,
    @SerializedName("horario_fim") val horarioFim: String?,
    @SerializedName("esforco_real") val esforcoReal: String?,
    @SerializedName("created_at") val createdAt: String
)

// ──────────────────────────────────────────────
// REGISTROS DE OCORRÊNCIA
// ──────────────────────────────────────────────

data class RegistroRequest(
    @SerializedName("especie_id") val especieId: Int,
    val data: String,
    val hora: String,
    val latitude: Double,
    val longitude: Double,
    @SerializedName("qtde_individuos") val qtdeIndividuos: Int? = null,
    /** Base64-encoded image */
    val foto: String? = null,
    @SerializedName("ausencia_especie") val ausenciaEspecie: Boolean? = null
)

data class RegistroPatchRequest(
    @SerializedName("especie_id") val especieId: Int? = null,
    val data: String? = null,
    val hora: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    @SerializedName("qtde_individuos") val qtdeIndividuos: Int? = null,
    /** Base64-encoded image */
    val foto: String? = null,
    @SerializedName("ausencia_especie") val ausenciaEspecie: Boolean? = null
)

data class RegistroResponse(
    val id: Int,
    @SerializedName("evento_amostragem_id") val eventoAmostragemId: Int,
    @SerializedName("especie_id") val especieId: Int,
    val data: String,
    val hora: String,
    val latitude: Double,
    val longitude: Double,
    @SerializedName("qtde_individuos") val qtdeIndividuos: Int?,
    /** Base64-encoded image */
    val foto: String?,
    @SerializedName("ausencia_especie") val ausenciaEspecie: Boolean?,
    @SerializedName("created_at") val createdAt: String
)
