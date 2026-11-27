package cl.duoc.ep3.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import cl.duoc.ep3.service.RewardPolicy;

@Entity
@Table(name = "productos")
public class Producto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String title;

    @Column(nullable = false, length = 2000)
    private String description;

    @Column(nullable = false)
    private Long price;

    @Column(nullable = false, length = 500)
    private String image;

    @Column(nullable = false, length = 80)
    private String category;

    @Column(length = 200)
    private String sourceId = "";

    @Column(length = 200)
    private String sourceRoute = "";

    @Column(length = 80)
    private String platform = "";

    @Column(length = 80)
    private String genre = "";

    @Column(length = 80)
    private String subcategory = "";

    public Producto() {
    }

    public Producto(String title, String description, Long price, String image,
                    String category, String sourceRoute, String platform,
                    String genre, String subcategory) {
        this.title = title;
        this.description = description;
        this.price = price;
        this.image = image;
        this.category = category;
        this.sourceRoute = sourceRoute;
        this.platform = platform;
        this.genre = genre;
        this.subcategory = subcategory;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Long getPrice() {
        return price;
    }

    public void setPrice(Long price) {
        this.price = price;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getSourceId() {
        return sourceId == null || sourceId.isBlank() ? String.valueOf(id) : sourceId;
    }

    public void setSourceId(String sourceId) {
        this.sourceId = sourceId;
    }

    @Transient
    public Integer getPuntosCanje() {
        return RewardPolicy.redemptionCost(getSourceId(), title, sourceRoute, category);
    }

    public String getSourceRoute() {
        return sourceRoute;
    }

    public void setSourceRoute(String sourceRoute) {
        this.sourceRoute = sourceRoute;
    }

    public String getPlatform() {
        return platform;
    }

    public void setPlatform(String platform) {
        this.platform = platform;
    }

    public String getGenre() {
        return genre;
    }

    public void setGenre(String genre) {
        this.genre = genre;
    }

    public String getSubcategory() {
        return subcategory;
    }

    public void setSubcategory(String subcategory) {
        this.subcategory = subcategory;
    }
}
