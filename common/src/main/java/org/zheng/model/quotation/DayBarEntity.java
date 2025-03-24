package org.zheng.model.quotation;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.zheng.model.support.AbstractBarEntity;

@Entity
@Table(name = "day_bars")
public class DayBarEntity extends AbstractBarEntity {

}
