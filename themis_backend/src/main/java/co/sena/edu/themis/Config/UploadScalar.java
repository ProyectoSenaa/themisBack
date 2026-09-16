package co.sena.edu.themis.config;

import graphql.language.StringValue;
import graphql.schema.*;
import org.springframework.web.multipart.MultipartFile;

public class UploadScalar {

    public static final GraphQLScalarType Upload = GraphQLScalarType.newScalar()
            .name("Upload")
            .description("A file upload scalar.")
            .coercing(new Coercing<Object, Object>() {
                @Override
                public Object serialize(Object dataFetcherResult) throws CoercingSerializeException {
                    return dataFetcherResult;
                }

                @Override
                public Object parseValue(Object input) throws CoercingParseValueException {
                    if (input instanceof MultipartFile) {
                        return input;
                    }
                    if (input instanceof String) {
                        return input;
                    }
                    throw new CoercingParseValueException("Invalid value for Upload");
                }

                @Override
                public Object parseLiteral(Object input) throws CoercingParseLiteralException {
                    if (input instanceof StringValue) {
                        return ((StringValue) input).getValue();
                    }
                    throw new CoercingParseLiteralException("Invalid literal for Upload");
                }
            })
            .build();
}
