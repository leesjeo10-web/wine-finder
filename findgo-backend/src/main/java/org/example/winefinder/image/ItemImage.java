package org.example.winefinder.image;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ItemImage {
    @Id
    @Column(length = 40)
    private String filename;
    @Column(nullable = false)
    private Long userId;

    public ItemImage(String filename, Long userId) {
        this.filename = filename;
        this.userId = userId;
    }
}
