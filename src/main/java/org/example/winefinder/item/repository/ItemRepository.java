package org.example.winefinder.item.repository;

import org.example.winefinder.item.entity.Item;
import org.example.winefinder.item.entity.ItemType;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ItemRepository extends JpaRepository<Item, Long> {
    @Query("""
            select i from Item i
            where (:type is null or i.type = :type)
              and (:pattern is null
                   or lower(i.name) like lower(:pattern) escape '!'
                   or lower(i.description) like lower(:pattern) escape '!')
            """)
    List<Item> search(@Param("pattern") String pattern, @Param("type") ItemType type, Sort sort);
}
