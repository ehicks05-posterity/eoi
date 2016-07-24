package net.ehicks.eoi;

import java.util.List;

public class PSIngredients
{
    public String query = "";
    public List<Object> args;

    public PSIngredients(String query, List<Object> args)
    {
        this.query = query;
        this.args = args;
    }
}
