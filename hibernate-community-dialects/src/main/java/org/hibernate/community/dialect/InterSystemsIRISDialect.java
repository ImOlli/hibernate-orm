/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.community.dialect;

import org.hibernate.engine.jdbc.dialect.spi.DialectResolutionInfo;

/**
 * A Hibernate dialect for InterSystems IRIS
 *
 * @author Oliver Stahl
 **/
public class InterSystemsIRISDialect extends CacheDialect {

  public InterSystemsIRISDialect() {
    super();
  }

  public InterSystemsIRISDialect(DialectResolutionInfo info) {
    super(info);
  }

  @Override
  public boolean supportsTableCheck() {
    return false;
  }

  @Override
  public boolean supportsColumnCheck() {
    return false;
  }

}