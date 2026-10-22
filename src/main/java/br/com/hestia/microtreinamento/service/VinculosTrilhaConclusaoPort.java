package br.com.hestia.microtreinamento.service;
import br.com.hestia.usuario.model.Usuario;
import java.util.List;
import java.util.Map;
/** A integração da SCRUM-491 fornece contexto à 527; não recalcula recorrência. */
public interface VinculosTrilhaConclusaoPort { List<Map<String,Object>> consultar(Usuario autor,Long conclusaoId); }
