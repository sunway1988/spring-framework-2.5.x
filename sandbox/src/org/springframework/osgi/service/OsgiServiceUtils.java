/*
 * Copyright 2002-2006 the original author or authors.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Created on 25-Jan-2006 by Adrian Colyer
 */
package org.springframework.osgi.service;

import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;

/**
 * Utility class offering easy access to OSGi services
 * 
 * @author Adrian Colyer
 * @since 2.0
 */
public class OsgiServiceUtils {

	/**
	 * Find the single OSGi service of the given type and matching the given filter.
	 * Throws an NoSuchServiceException if there are no
	 * matching services, or AmbiguousServiceReferenceException if there are more than
	 * one candidate matches.
	 * @param context
	 * @param serviceClass
	 * @param filter
	 * @return the service of the given type matching the given filter
	 * 
	 * @throws NoSuchServiceException if a matching service cannot be found
	 * @throws AmbiguousServiceReferenceException if multiple matching services are found
	 * @throws IllegalArgumentException if the filter string is non-null and is not well-formed
	 */
	public static ServiceReference getService(BundleContext context, Class serviceClass, String filter)
	throws NoSuchServiceException, AmbiguousServiceReferenceException, IllegalArgumentException 
	{
		try {
			ServiceReference[] serviceReferences = 
				context.getServiceReferences(serviceClass.getName(),filter);
			if (serviceReferences.length == 0) {
				throw new NoSuchServiceException(
						"A service of type '" + serviceClass.getName() + "' matching filter '" +
						((filter == null) ? "" : filter) + "' could not be found.",
						serviceClass, filter);
			}
			else if (serviceReferences.length > 1) {
				throw new AmbiguousServiceReferenceException(
						"Found " + serviceReferences.length + " services of type '" + serviceClass.getName() + 
						"' matching filter '" +
						((filter == null) ? "" : filter) + "' (expecting only one)",
						serviceClass, filter);
			}
			else {
				return serviceReferences[0];
			}
		}
		catch (InvalidSyntaxException ex) {
			throw new IllegalArgumentException(ex.getMessage(),ex);
		}
	}
	
	/**
	 * Return all of the service references for services of the given type and matching
	 * the given filter
	 * @param context
	 * @param serviceClass
	 * @param filter
	 * @return
	 * 
	 * @throws IllegalArgumentException if the filter string is non-null and is not well-formed
	 */
	public static ServiceReference[] getServices(BundleContext context, Class serviceClass, String filter) 
	throws IllegalArgumentException 
	{
		try {
			ServiceReference[] serviceReferences = 
				context.getServiceReferences(serviceClass.getName(),filter);
			return serviceReferences;
		}
		catch (InvalidSyntaxException ex) {
			throw new IllegalArgumentException(ex.getMessage(),ex);
		}
	}
	
}
