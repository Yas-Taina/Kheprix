export function extrairMensagemErro(
  err: any,
  fallback = "Ocorreu um erro inesperado. Tente novamente.",
): string {
  const corpo = err?.error;
  if (!corpo) return fallback;

  if (typeof corpo.erro === "string" && corpo.erro.trim()) {
    return corpo.erro;
  }

  if (Array.isArray(corpo.erros) && corpo.erros.length > 0) {
    return corpo.erros.join("; ");
  }

  if (typeof corpo.message === "string" && corpo.message.trim()) {
    return corpo.message;
  }

  if (err?.status === 0) {
    return "Sem conexão com o servidor. Verifique sua rede.";
  }

  if (err?.status === 401) {
    return "Sessão expirou. Faça login novamente.";
  }

  if (err?.status === 403) {
    return "Você não tem permissão para essa ação.";
  }

  if (err?.status === 404) {
    return "Recurso não encontrado.";
  }

  return fallback;
}
