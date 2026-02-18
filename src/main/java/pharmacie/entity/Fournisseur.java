package pharmacie.entity;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.ToString;


@Entity
@Getter @Setter @ToString

public class Fournisseur {
    @Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Setter(AccessLevel.NONE) // la clé est autogénérée par la BD, On ne veut pas de "setter"
	private Integer id_fournisseur = null;

    @NonNull // Lombok, génère une vérification dans le constructeur par défaut
	@Column(unique=true, length = 255)
    private String nom_fournisseur;

    @NonNull // Lombok, génère une vérification dans le constructeur par défaut
	@Column(unique=true, length = 255)
    private String mail_fournisseur;

    @ToString.Exclude
	@ManyToMany(mappedBy = "fournisseurs")
    @JsonIgnoreProperties("fournisseurs") 
    private List<Categorie> categories = new ArrayList<>();
}
