class CriarEventoAmostragemController < ApplicationController
    before_action :autenticar_requisicao!
    before_action :carregar_evento, only: %i[show update destroy]

    def index
        eventos = servico.listar
        render json: eventos, status: :ok
    end
    
    def show
        render json: @evento, status: :ok
    end

    def create
        dto = CriarEventoAmostragemDto.new(params)

        unless dto.valid?
            render json: { erros: dto.errors.full_messages }, status: :unprocessable_entity
            return
        end

        evento = servico.criar(
            horario_inicio: dto.horario_inicio,
            horario_fim: dto.horario_fim,
            esforco_real: dto.esforco_real,
        )
    if evento.persisted?
      render json: evento, status: :created
    else
      render json: { erros: evento.errors.full_messages }, status: :unprocessable_entity
    end
  end

  def update
    dto = AtualizarEventoAmostragemDto.new(params)

    unless dto.valid?
      render json: { erros: dto.errors.full_messages }, status: :unprocessable_entity
      return
    end

    evento = servico.atualizar(
      evento: @evento,
      horario_inicio: dto.horario_inicio,
      horario_fim: dto.horario_fim,
      esforco_real: dto.esforco_real,
    )

    if evento.errors.empty?
      render json: evento, status: :ok
    else
      render json: { erros: evento.errors.full_messages }, status: :unprocessable_entity
    end
  end

  def destroy
    servico.destruir(@evento)
    head :no_content
  end

  private

  def servico
    @servico ||= ServicoEventoAmostragem.new
  end

  def carregar_evento
    @evento = servico.buscar_por_id(params[:id])

    unless @evento
      render json: { erro: "Evento de amostragem não encontrado" }, status: :not_found
    end
  end
end