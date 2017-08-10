package org.datavec.dataframe.filtering;

import org.datavec.dataframe.api.CategoryColumn;
import org.datavec.dataframe.columns.ColumnReference;

/**
 * @deprecated use {@link StringIsNotIn} instead.  
 */
@Deprecated
public class StringNotInSet extends StringIsNotIn {

    public StringNotInSet(ColumnReference reference, CategoryColumn filterColumn) {
        super(reference, filterColumn);
    }

}
