package org.inferred.freebuilder.processor;

import static org.inferred.freebuilder.processor.util.ModelUtils.findAnnotationMirror;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableSet;

import org.inferred.freebuilder.processor.Metadata.Property;
import org.inferred.freebuilder.processor.util.Excerpt;
import org.inferred.freebuilder.processor.util.QualifiedName;
import org.inferred.freebuilder.processor.util.SourceBuilder;

import java.util.Set;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;

class JacksonSupport {

  private static final String JSON_DESERIALIZE =
      "com.fasterxml.jackson.databind.annotation.JsonDeserialize";
  private static final QualifiedName JSON_PROPERTY =
      QualifiedName.of("com.fasterxml.jackson.annotation", "JsonProperty");
  /** Annotations which disable automatic generation of JsonProperty annotations. */
  private static final Set<QualifiedName> DISABLE_PROPERTY_ANNOTATIONS = ImmutableSet.of(
      QualifiedName.of("com.fasterxml.jackson.annotation", "JsonAnyGetter"),
      QualifiedName.of("com.fasterxml.jackson.annotation", "JsonIgnore"),
      QualifiedName.of("com.fasterxml.jackson.annotation", "JsonUnwrapped"),
      QualifiedName.of("com.fasterxml.jackson.annotation", "JsonValue"));

  public static Optional<JacksonSupport> create(TypeElement userValueType) {
    if (findAnnotationMirror(userValueType, JSON_DESERIALIZE).isPresent()) {
      return Optional.of(new JacksonSupport());
    }
    return Optional.absent();
  }

  private JacksonSupport() {}

  public void addJacksonAnnotations(
      Property.Builder resultBuilder, ExecutableElement getterMethod) {
    Optional<AnnotationMirror> annotation = findAnnotationMirror(getterMethod, JSON_PROPERTY);
    if (annotation.isPresent()) {
      resultBuilder.addAccessorAnnotations(new AnnotationExcerpt(annotation.get()));
    } else if (generateDefaultAnnotations(getterMethod)) {
      resultBuilder.addAccessorAnnotations(new JsonPropertyExcerpt(resultBuilder.getName()));
    }
  }

  private static boolean generateDefaultAnnotations(ExecutableElement getterMethod) {
    for (AnnotationMirror annotationMirror : getterMethod.getAnnotationMirrors()) {
      TypeElement annotationTypeElement =
          (TypeElement) (annotationMirror.getAnnotationType().asElement());
      QualifiedName annotationType = QualifiedName.of(annotationTypeElement);
      if (DISABLE_PROPERTY_ANNOTATIONS.contains(annotationType)) {
        return false;
      }
    }
    return true;
  }

  private static class AnnotationExcerpt implements Excerpt {

    private final AnnotationMirror annotation;

    AnnotationExcerpt(AnnotationMirror annotation) {
      this.annotation = annotation;
    }

    @Override
    public void addTo(SourceBuilder code) {
      code.addLine("%s", annotation);
    }
  }

  private static class JsonPropertyExcerpt implements Excerpt {

    private final String propertyName;

    JsonPropertyExcerpt(String propertyName) {
      this.propertyName = propertyName;
    }

    @Override
    public void addTo(SourceBuilder code) {
      code.addLine("@%s(\"%s\")", JSON_PROPERTY, propertyName);
    }
  }

}
