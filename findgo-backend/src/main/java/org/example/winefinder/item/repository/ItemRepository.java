package org.example.winefinder.item.repository;

import org.example.winefinder.item.entity.*;
import org.springframework.data.jpa.repository.*;

public interface ItemRepository extends JpaRepository<Item, Long>, JpaSpecificationExecutor<Item> {
    long countByType(ItemType type);
    long countByStatus(ItemStatus status);
}
