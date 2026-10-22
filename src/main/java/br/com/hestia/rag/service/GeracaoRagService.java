package br.com.hestia.rag.service;
import java.util.List;
public interface GeracaoRagService {
    record Gerada(String texto,String modelo,Long tokensEntrada,Long tokensSaida){}
    Gerada gerar(String pergunta,List<String> trechosAutorizados);
}
