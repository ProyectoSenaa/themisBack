package co.sena.edu.themis.Utils.scalar;

import com.netflix.graphql.dgs.DgsScalar;
import graphql.GraphQLContext;
import graphql.language.StringValue;
import graphql.language.Value;
import graphql.schema.Coercing;

import graphql.schema.CoercingParseValueException;
import graphql.schema.CoercingSerializeException;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

@DgsScalar(name = "Long")
public class LongScalar implements Coercing<Long, String> {

    @Override
    public String serialize( @NotNull Object dataFetcherResult,@NotNull GraphQLContext graphQLContext ,@NotNull Locale locale) throws CoercingSerializeException {
        if (dataFetcherResult instanceof Long) {
            return dataFetcherResult.toString();
        } else {
            throw new CoercingSerializeException("Not a valid Long");
        }
    }

    @Override
    public Long parseValue(Object input,@NotNull GraphQLContext graphQLContext ,@NotNull Locale locale) throws CoercingParseValueException {
        try {
            return Long.parseLong(input.toString());
        } catch (NumberFormatException e) {
            throw new CoercingParseValueException("Invalid value for Long: " + input);
        }
    }

    @Override
    public @NotNull Value<?> valueToLiteral(@NotNull Object input, @NotNull GraphQLContext context, @NotNull Locale locale) {
        return new StringValue(serialize(input, context, locale));
    }
}