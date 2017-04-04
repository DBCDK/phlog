package dk.dbc.phlog.dto;

import com.fasterxml.jackson.databind.type.MapType;
import dk.dbc.commons.jsonb.JSONBContext;
import dk.dbc.commons.jsonb.JSONBException;
import org.postgresql.util.PGobject;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;
import java.sql.SQLException;
import java.util.Map;

@Converter
public class StatusMapConverter implements AttributeConverter<Map<String, Integer>, PGobject> {
    private final static JSONBContext JSONB_CONTEXT = new JSONBContext();

    @Override
    public PGobject convertToDatabaseColumn(Map<String, Integer> map) throws IllegalStateException {
        final PGobject pgObject = new PGobject();
        pgObject.setType("jsonb");
        try {
            pgObject.setValue(JSONB_CONTEXT.marshall(map));
        } catch(SQLException | JSONBException e) {
            throw new IllegalStateException(e);
        }
        return pgObject;
    }

    @Override
    public Map<String, Integer> convertToEntityAttribute(PGobject pgObject) throws IllegalStateException {
        if(pgObject == null) return null;
        try {
            final MapType mapType = JSONB_CONTEXT.getTypeFactory()
                .constructMapType(Map.class, String.class, Integer.class);
            return JSONB_CONTEXT.unmarshall(pgObject.getValue(), mapType);
        } catch(JSONBException e) {
            throw new IllegalStateException(e);
        }
    }
}
