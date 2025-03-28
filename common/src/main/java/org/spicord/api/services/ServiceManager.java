/*
 * Copyright (C) 2020  OopsieWoopsie
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package org.spicord.api.services;

import org.spicord.api.services.linking.LinkingService;

public interface ServiceManager {

    /**
     * Registers a new service.
     * <br>
     * Example: registerService(LinkingService.class, myLinkingServiceInstance);
     * 
     * @param serviceClass the type of service provided
     * @param service your service instance
     * @return true if the service was successfully registered
     */
    boolean registerService(Class<? extends Service> serviceClass, Service service);

    /**
     * Checks if a service provider was registered.
     * <br>
     * For example {@code isServiceRegistered(LinkingService.class)} will return true if a provider for {@link LinkingService} is registered.
     * 
     * @param serviceClass the service to be checked
     * @return true if the service is registered
     */
    boolean isServiceRegistered(Class<? extends Service> serviceClass);

    /**
     * Unregisters a previously registered service.
     * 
     * @param serviceClass the class of the service to be unregistered
     * @return true if the service was successfully unregistered
     */
    boolean unregisterService(Class<? extends Service> serviceClass);

    /**
     * Unregisters a previously registered service.
     * 
     * @param service the instance of the service to be unregistered
     * @return true if the service was successfully unregistered
     */
    boolean unregisterService(Service service);

    /**
     * Get a service instance by its class.
     * 
     * @param <T> the service instance type
     * @param serviceClass the class of the service
     * @return the service instance
     */
    <T extends Service> T getService(Class<? extends Service> serviceClass);

    /**
     * Get a service instance by its id.
     * 
     * @param <T> the service instance type
     * @param id the id of the service
     * @return the service instance
     */
    <T extends Service> T getService(String id);
}
