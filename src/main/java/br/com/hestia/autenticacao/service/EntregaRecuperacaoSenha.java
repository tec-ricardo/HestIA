package br.com.hestia.autenticacao.service;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
@Service
public class EntregaRecuperacaoSenha {
 private final ObjectProvider<JavaMailSender> sender; private final String remetente; private final String base;
 public EntregaRecuperacaoSenha(ObjectProvider<JavaMailSender> sender,
   @Value("${hestia.recuperacao.remetente:}") String remetente,
   @Value("${hestia.recuperacao.url-publica:}") String base){this.sender=sender;this.remetente=remetente;this.base=base;}
 public void enviar(String email,String token){
  var mail=sender.getIfAvailable();
  if(mail==null||remetente.isBlank()||!base.startsWith("https://"))throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Canal de recuperação indisponível");
  var mensagem=new SimpleMailMessage();mensagem.setFrom(remetente);mensagem.setTo(email);
  mensagem.setSubject("HestIA — Recuperação de acesso");
  mensagem.setText("Foi solicitada a recuperação de sua conta. O link vale por 30 minutos e só pode ser usado uma vez.\n\n"+base+"/redefinir-senha?token="+java.net.URLEncoder.encode(token,java.nio.charset.StandardCharsets.UTF_8)+"\n\nSe você não solicitou, ignore esta mensagem.");
  try{mail.send(mensagem);}catch(org.springframework.mail.MailException e){throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Não foi possível enviar as instruções. Tente novamente mais tarde.");}
 }
}
