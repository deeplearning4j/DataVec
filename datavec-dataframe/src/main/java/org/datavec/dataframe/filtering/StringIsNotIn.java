package org.datavec.dataframe.filtering;

import java.util.Collection;
import java.util.Set;

import org.datavec.dataframe.api.CategoryColumn;
import org.datavec.dataframe.api.Table;
import org.datavec.dataframe.columns.ColumnReference;
import org.datavec.dataframe.util.Selection;

import com.google.common.collect.Lists;

/**
 * Implements NotEqualTo testing for Category and Text Columns
 */
public class StringIsNotIn extends ColumnFilter {

    private CategoryColumn filterColumn;

    public StringIsNotIn(ColumnReference reference, CategoryColumn filterColumn) {
        super(reference);
        this.filterColumn = filterColumn;
    }

    public StringIsNotIn(ColumnReference reference, Collection<String> strings) {
      super(reference);
      this.filterColumn = CategoryColumn.create("temp", Lists.newArrayList(strings));
    }
 
    public StringIsNotIn(ColumnReference reference, String... strings) {
        super(reference);
        this.filterColumn = CategoryColumn.create("temp", Lists.newArrayList(strings));
    }

    public Selection apply(Table relation) {
        CategoryColumn categoryColumn = (CategoryColumn) relation.column(columnReference.getColumnName());
        Set<String> firstSet = categoryColumn.asSet();
        firstSet.removeAll(filterColumn.data());
        return categoryColumn.select(firstSet::contains);
    }
}
