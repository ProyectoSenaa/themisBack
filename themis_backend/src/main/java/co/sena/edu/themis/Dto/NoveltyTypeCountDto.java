package co.sena.edu.themis.Dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class NoveltyTypeCountDto {
    private Long id;

    private String nameNovelty;
    private int count;

    public NoveltyTypeCountDto(Long id, String nameNovelty, int count) {
        this.id = id;
        this.nameNovelty = nameNovelty;
        this.count = count;
    }

    public NoveltyTypeCountDto(String key, Long value) {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNameNovelty() {
        return nameNovelty;
    }

    public void setNameNovelty(String nameNovelty) {
        this.nameNovelty = nameNovelty;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }
}
