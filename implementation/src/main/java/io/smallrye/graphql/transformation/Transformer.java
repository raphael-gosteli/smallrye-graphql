package io.smallrye.graphql.transformation;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.text.ParseException;
import java.time.DateTimeException;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Optional;

import io.smallrye.graphql.execution.Classes;
import io.smallrye.graphql.schema.model.Field;
import io.smallrye.graphql.schema.model.TransformInfo;

/**
 * Transform data.
 * Format on the way out and Parse on the way in
 * 
 * TODO: Consider caching created transformers ?
 * 
 * @author Phillip Kruger (phillip.kruger@redhat.com)
 */
public class Transformer {

    private DateTimeFormatter dateTimeFormatter = null;
    private NumberFormat numberFormat = null;
    private DateTransformer dateTransformer;
    private NumberTransformer numberTransformer;

    private final Field field;

    public static Transformer transformer(Field field) {
        if (field.getTransformInfo().isPresent()) {
            return new Transformer(field);
        }
        return null;
    }

    private Transformer(Field field) {
        this.field = field;
        init(field.getTransformInfo());
    }

    private void init(Optional<TransformInfo> maybeFormat) {
        if (maybeFormat.isPresent()) {
            TransformInfo format = maybeFormat.get();

            if (format.getType().equals(TransformInfo.Type.NUMBER)) {
                this.numberFormat = getNumberFormat(format);
                this.numberTransformer = NumberTransformer.transformer(numberFormat);
            } else if (format.getType().equals(TransformInfo.Type.DATE)) {
                this.dateTimeFormatter = getDateFormat(format);
                this.dateTransformer = DateTransformer.transformer(dateTimeFormatter);
            }
        }
    }

    // Parsing (on the way in)

    /**
     * Check if the input is valid and then parse with the correct transformer
     * 
     * @param input the value to be transformed
     * @return the transformed result
     */
    public Object parseInput(Object input) throws ParseException, NumberFormatException, DateTimeException {

        if (Classes.isCollection(input)) {
            throw new RuntimeException("Can not parse [" + input + "] of type [" + input.getClass().getName() + "]");
        } else if (input != null && dateTimeFormatter != null) {
            return parseDateInput(input);
        } else if (input != null && numberFormat != null) {
            return parseNumberInput(input);
        }
        return input; // default
    }

    /**
     * Parse a date input
     * 
     * @param input the formatted date
     * @return date
     */
    private Object parseDateInput(Object input) {
        String className = field.getReference().getClassName();
        return dateTransformer.stringToDateType(input.toString(), className).orElse(input);
    }

    /**
     * Parse a number input
     * 
     * @param input the formatted number
     * @return number
     */
    private Object parseNumberInput(Object input) throws ParseException, NumberFormatException {
        String className = field.getReference().getClassName();
        return numberTransformer.stringToNumberType(input.toString(), className).orElse(input);
    }

    // Formatting (on the way out)

    /**
     * Check if the output is valid and then format with the correct transformer
     * 
     * @param output the value to be transformed
     * @return the transformed result
     */
    public Object formatOutput(Object output) {
        if (Classes.isCollection(output)) {
            throw new RuntimeException("Can not format [" + output + "] of type [" + output.getClass().getName() + "]");
        } else if (output != null && dateTimeFormatter != null) {
            return formatDateOutput(output);
        } else if (output != null && numberFormat != null) {
            return formatNumberObject(output);
        }
        return output; // default
    }

    /**
     * Format a date
     * 
     * @param output the date
     * @return formatted result
     */
    private Object formatDateOutput(Object output) {
        return dateTransformer.dateTypeToString(output);
    }

    /**
     * Format a number
     * 
     * @param output the number
     * @return formatted result
     */
    private Object formatNumberObject(Object output) {
        if (Number.class.isInstance(output)) {
            Number number = (Number) output;
            return numberFormat.format(number);
        } else {
            return output;
        }
    }

    private DateTimeFormatter getDateFormat(TransformInfo formatter) {
        if (formatter != null) {
            String format = formatter.getFormat();
            String locale = formatter.getLocale();
            if (format == null) {
                return null;
            } else if (locale == null) {
                return DateTimeFormatter.ofPattern(format);
            } else {
                return DateTimeFormatter.ofPattern(format).withLocale(Locale.forLanguageTag(locale));
            }
        }
        return null;
    }

    private NumberFormat getNumberFormat(TransformInfo formatter) {
        if (formatter != null) {
            String format = formatter.getFormat();
            String locale = formatter.getLocale();

            if (format == null && locale == null) {
                return null;
            } else if (format == null) {
                return NumberFormat.getInstance(Locale.forLanguageTag(locale));
            } else if (locale == null) {
                return new DecimalFormat(format);
            } else {
                return new DecimalFormat(format,
                        DecimalFormatSymbols.getInstance(Locale.forLanguageTag(locale)));
            }
        }
        return null;
    }
}
