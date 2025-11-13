package vaccinesdistribution.Service;

import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;

import vaccinesdistribution.Model.Warehouse;
import vaccinesdistribution.Model.WarehouseIdentifier;
import vaccinesdistribution.Model.VaccineBatch;
import vaccinesdistribution.Util.Point;

public class Storage {
    private static final Gson gson = new GsonBuilder()
            .setPrettyPrinting()
            .registerTypeAdapter(Warehouse.class, new WarehouseDeserializer())
            .create();

    public static List<Warehouse> loadWarehousesFromJsonFile(String filePath) throws IOException, IllegalArgumentException {
        try (Reader reader = new FileReader(filePath)) {
            Type warehouseListType = new TypeToken<List<Warehouse>>(){}.getType();
            List<Warehouse> warehouses = gson.fromJson(reader, warehouseListType);
            
            if (warehouses == null) {
                return new ArrayList<>();
            }
            
            return warehouses;
        } catch (JsonSyntaxException e) {
            throw new IllegalArgumentException("Invalid JSON format: " + e.getMessage(), e);
        }
    }

    private static class WarehouseDeserializer implements JsonDeserializer<Warehouse> {
        @Override
        public Warehouse deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) 
                throws JsonParseException {
            
            JsonObject jsonObject = json.getAsJsonObject();
    
            if (!jsonObject.has("name")) {
                throw new JsonParseException("Missing required field: name");
            }
            String name = jsonObject.get("name").getAsString();

            if (!jsonObject.has("x_coordinate")) {
                throw new JsonParseException("Missing required field: x_coordinate");
            }
            if (!jsonObject.has("y_coordinate")) {
                throw new JsonParseException("Missing required field: y_coordinate");
            }
            int x = jsonObject.get("x_coordinate").getAsInt();
            int y = jsonObject.get("y_coordinate").getAsInt();
            
            Point location = new Point(x, y);
            WarehouseIdentifier identifier = new WarehouseIdentifier(name, location);
            Warehouse warehouse = new Warehouse(identifier);
    
            if (!jsonObject.has("vaccine_batches")) return warehouse;

            if (!jsonObject.get("vaccine_batches").isJsonArray()) {
                throw new JsonParseException("The vaccine_batches field must be an array");
            }
            JsonArray vaccineBatchesArray = jsonObject.getAsJsonArray("vaccine_batches");

            VaccineBatch batch;
            JsonObject batchObject;
            int batchSize, expirationDate;
            for (JsonElement element : vaccineBatchesArray) {
                if (element.isJsonNull() || !element.isJsonObject()) continue;

                batchObject = element.getAsJsonObject();
                
                if (!batchObject.has("batch_size")) {
                    throw new JsonParseException("Missing required field: batch_size in vaccine_batches");
                }
                if (!batchObject.has("expiration_date")) {
                    throw new JsonParseException("Missing required field: expiration_date in vaccine_batches");
                }
                
                batchSize = batchObject.get("batch_size").getAsInt();
                expirationDate = batchObject.get("expiration_date").getAsInt();
                
                batch = new VaccineBatch(batchSize, expirationDate);
                warehouse.registerPerishableBatch(batch);
            }
    
            return warehouse;
        }
    }
}
