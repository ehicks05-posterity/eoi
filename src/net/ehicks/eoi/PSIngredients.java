package net.ehicks.eoi;

import java.util.ArrayList;
import java.util.List;

public class PSIngredients
{
    public String query = "";
    public List<Object> args;
    public List<UpdatedField> updatedFields = new ArrayList<>();

    public PSIngredients(String query, List<Object> args)
    {
        this.query = query;
        this.args = args;
    }

    public PSIngredients(String query, List<Object> args, List<UpdatedField> updatedFields)
    {
        this.query = query;
        this.args = args;
        this.updatedFields = updatedFields;
    }

    static class UpdatedField
    {
        String fieldName;
        Object oldValue;
        Object newValue;

        public UpdatedField(String fieldName, Object oldValue, Object newValue)
        {
            this.fieldName = fieldName;
            this.oldValue = oldValue;
            this.newValue = newValue;
        }
    }
}
