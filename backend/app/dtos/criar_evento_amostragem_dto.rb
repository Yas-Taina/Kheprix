class CriarEventoAmostragemDto
  include ActiveModel::API 

  attr_accessor :horario_inicio, :horario_fim, :esforco_real

    validates :horario_inicio, presence: true

    def initialize(params = {})
        @horario_inicio = params[:horario_inicio]
        @horario_fim = params[:horario_fim]
        @esforco_real = params[:esforco_real]
    end
end