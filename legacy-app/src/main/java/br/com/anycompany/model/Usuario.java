package br.com.anycompany.model;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "Usuario")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private String login;
    private String senhaMd5;
    private String nome;
    private String perfil;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getLogin() { return login; }
    public void setLogin(String login) { this.login = login; }
    public String getSenhaMd5() { return senhaMd5; }
    public void setSenhaMd5(String senhaMd5) { this.senhaMd5 = senhaMd5; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public String getPerfil() { return perfil; }
    public void setPerfil(String perfil) { this.perfil = perfil; }
}
