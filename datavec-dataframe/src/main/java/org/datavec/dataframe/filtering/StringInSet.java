package org.datavec.dataframe.filtering;

import org.datavec.dataframe.api.CategoryColumn;
import org.datavec.dataframe.columns.ColumnReference;

/**
 * @deprecated use {@link StringIsIn} instead.  
 */
@Deprecated
public class StringInSet extends StringIsIn {

    public StringInSet(ColumnReference reference, CategoryColumn filterColumn) {
        super(reference, filterColumn);
    }

}
