class AtualizarEventoAmostragem
    def listarEventoAmostrage
        EventoAmostragem.ordenados
    end

    # Busca um evento de amostragem pelo ID
    def buscar_por_id(id)
        EventoAmostragem.find_by(id: id)
    end

    #Cria novo evento de amostragem
    def criar(horario_inicio:, horario_fim:, esforco_real:)
        evento.update(
            horario_inicio: horario_inicio,
            horario_fim: horario_fim,
            esforco_real: esforco_real,
        )
    end
    
    # Atualiza evento existente de amostragem
    def atualizar(evento, horario_inicio:, horario_fim:, esforco_real:)
        evento.update(
            horario_inicio: horario_inicio,
            horario_fim: horario_fim,
            esforco_real: esforco_real,
        )
        evento
    end
    
    #Exclui evento de amostragem
    def excluir(evento)
        evento.destroy
    end