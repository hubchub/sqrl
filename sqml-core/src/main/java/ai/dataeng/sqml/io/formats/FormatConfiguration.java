package ai.dataeng.sqml.io.formats;

import ai.dataeng.sqml.config.error.ErrorCollector;
import ai.dataeng.sqml.io.impl.InputPreview;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.io.Serializable;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "formatType")
@JsonSubTypes({ @JsonSubTypes.Type(value = CSVFormat.Configuration.class, name = CSVFormat.NAME),
        @JsonSubTypes.Type(value = JsonLineFormat.Configuration.class, name = JsonLineFormat.NAME), })
public interface FormatConfiguration extends Serializable {

    boolean initialize(InputPreview preview, ErrorCollector errors);

    @JsonIgnore
    FileFormat getFileFormat();

    @JsonIgnore
    Format getImplementation();

    @JsonIgnore
    String getName();

}
