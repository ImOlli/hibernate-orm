/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.metamodel.mapping;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

import org.hibernate.AssertionFailure;
import org.hibernate.HibernateException;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.mapping.internal.EmbeddableDiscriminatorValueDetailsImpl;
import org.hibernate.metamodel.model.domain.NavigableRole;
import org.hibernate.type.BasicType;
import org.hibernate.type.descriptor.java.JavaType;

/**
 * Handles conversion of discriminator values for embeddable subtype classes
 * to their domain typed form.
 *
 * @author Marco Belladelli
 * @see EmbeddableDiscriminatorMapping
 */
public class EmbeddableDiscriminatorConverter<O, R> extends DiscriminatorConverter<O, R> {
	public static <O, R> EmbeddableDiscriminatorConverter<O, R> fromValueMappings(
			String discriminatedType,
			JavaType<O> domainJavaType,
			BasicType<R> underlyingJdbcMapping,
			Map<Object, String> valueMappings,
			SessionFactoryImplementor sessionFactory) {
		final List<EmbeddableDiscriminatorValueDetailsImpl> valueDetailsList = new ArrayList<>( valueMappings.size() );
		final ClassLoaderService cls = sessionFactory.getServiceRegistry().requireService( ClassLoaderService.class );
		valueMappings.forEach( (value, embeddableClassName) -> valueDetailsList.add( new EmbeddableDiscriminatorValueDetailsImpl(
				value,
				cls.classForName( embeddableClassName )
		) ) );
		return new EmbeddableDiscriminatorConverter<>(
				discriminatedType,
				domainJavaType,
				underlyingJdbcMapping.getJavaTypeDescriptor(),
				valueDetailsList
		);
	}

	private final Map<Object, EmbeddableDiscriminatorValueDetailsImpl> discriminatorValueToDetailsMap;
	private final Map<String, EmbeddableDiscriminatorValueDetailsImpl> embeddableClassNameToDetailsMap;

	public EmbeddableDiscriminatorConverter(
			String discriminatorName,
			JavaType<O> domainJavaType,
			JavaType<R> relationalJavaType,
			List<EmbeddableDiscriminatorValueDetailsImpl> valueMappings) {
		super( discriminatorName, domainJavaType, relationalJavaType );

		this.discriminatorValueToDetailsMap = new HashMap<>( valueMappings.size() );
		this.embeddableClassNameToDetailsMap = new HashMap<>( valueMappings.size() );
		valueMappings.forEach( valueDetails -> {
			discriminatorValueToDetailsMap.put( valueDetails.getValue(), valueDetails );
			embeddableClassNameToDetailsMap.put( valueDetails.getIndicatedEntityName(), valueDetails );
		} );
	}

	@Override
	public O toDomainValue(R relationalForm) {
		assert relationalForm == null || getRelationalJavaType().isInstance( relationalForm );

		final EmbeddableDiscriminatorValueDetailsImpl matchingValueDetails = getDetailsForDiscriminatorValue( relationalForm );
		if ( matchingValueDetails == null ) {
			throw new IllegalStateException( "Could not resolve discriminator value" );
		}

		//noinspection unchecked
		return (O) matchingValueDetails.getEmbeddableClass();
	}

	@Override
	public EmbeddableDiscriminatorValueDetailsImpl getDetailsForDiscriminatorValue(Object value) {
		final EmbeddableDiscriminatorValueDetailsImpl valueMatch = discriminatorValueToDetailsMap.get( value );
		if ( valueMatch != null ) {
			return valueMatch;
		}

		throw new HibernateException( "Unrecognized discriminator value: " + value );
	}

	@Override
	public DiscriminatorValueDetails getDetailsForEntityName(String embeddableClassName) {
		final DiscriminatorValueDetails valueDetails = embeddableClassNameToDetailsMap.get( embeddableClassName );
		if ( valueDetails != null ) {
			return valueDetails;
		}

		throw new AssertionFailure( "Unrecognized embeddable class: " + embeddableClassName );
	}

	@Override
	public void forEachValueDetail(Consumer<DiscriminatorValueDetails> consumer) {
		discriminatorValueToDetailsMap.forEach( (value, detail) -> consumer.accept( detail ) );
	}

	@Override
	public <X> X fromValueDetails(Function<DiscriminatorValueDetails, X> handler) {
		for ( DiscriminatorValueDetails detail : discriminatorValueToDetailsMap.values() ) {
			final X result = handler.apply( detail );
			if ( result != null ) {
				return result;
			}
		}
		return null;
	}
}
