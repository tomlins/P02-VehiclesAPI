package com.udacity.vehicles.service;

import com.udacity.vehicles.client.maps.Address;
import com.udacity.vehicles.client.prices.Price;
import com.udacity.vehicles.domain.Location;
import com.udacity.vehicles.domain.car.Car;
import com.udacity.vehicles.domain.car.CarRepository;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;

/**
 * Implements the car service create, read, update or delete
 * information about vehicles, as well as gather related
 * location and price data when desired.
 */
@Service
public class CarService {

    private static final int NUMBER_OF_PRICE_RECORDS = 20;

    private final CarRepository repository;

    private WebClient maps;
    private WebClient pricing;
    private ModelMapper mapper;

    public CarService(CarRepository repository, WebClient maps, WebClient pricing, ModelMapper mapper) {
        /**
         * TODO: Add the Maps and Pricing Web Clients you create
         *   in `VehiclesApiApplication` as arguments and set them here.
         */
        this.maps = maps;
        this.pricing = pricing;

        this.repository = repository;
        this.mapper = mapper;
    }

    /**
     * Gathers a list of all vehicles
     * @return a list of all vehicles in the CarRepository
     */
    public List<Car> list() {
        return repository.findAll();
    }

    /**
     * Gets car information by ID (or throws exception if non-existent)
     * @param id the ID number of the car to gather information on
     * @return the requested car's information, including location and price
     */
    public Car findById(Long id) {
        /**
         * TODO: Find the car by ID from the `repository` if it exists.
         *   If it does not exist, throw a CarNotFoundException
         *   Remove the below code as part of your implementation.
         */
        Optional<Car> optionalCar = repository.findById(id);
        if (optionalCar.isEmpty())
            throw new CarNotFoundException("Car with id " + id + " was not found!");
        Car car = optionalCar.get();

        /**
         * TODO: Use the Pricing Web client you create in `VehiclesApiApplication`
         *   to get the price based on the `id` input'
         * TODO: Set the price of the car
         * Note: The car class file uses @transient, meaning you will need to call
         *   the pricing service each time to get the price.
         */

        // Substitute a random Car id to use instead of the actual Car id to get
        // back a random Price within the range of id's pre-loaded in the database
        // This way we will never run out of Prices should we generate more Cars
        // than there are Price records.
        int randomVehicleId = new Random().nextInt(NUMBER_OF_PRICE_RECORDS - 1) + 1;

        Price price = pricing
                .get()
                .uri("prices/" + randomVehicleId)
                .retrieve()
                .bodyToMono(Price.class)
                .block();

        car.setPrice(String.format("%s %s", price.getCurrency(), price.getPrice()));


        /**
         * TODO: Use the Maps Web client you create in `VehiclesApiApplication`
         *   to get the address for the vehicle. You should access the location
         *   from the car object and feed it to the Maps service.
         * TODO: Set the location of the vehicle, including the address information
         * Note: The Location class file also uses @transient for the address,
         * meaning the Maps service needs to be called each time for the address.
         */
        Location carLocation = car.getLocation();
        Address address = maps
                .get()
                .uri(uriBuilder -> uriBuilder
                        .path("maps/")
                        .queryParam("lat", carLocation.getLat())
                        .queryParam("lon", carLocation.getLon())
                        .build())
                .retrieve()
                .bodyToMono(Address.class)
                .block();

        mapper.map(Objects.requireNonNull(address), carLocation);
        car.setLocation(carLocation);

        return car;
    }

    /**
     * Either creates or updates a vehicle, based on prior existence of car
     * @param car A car object, which can be either new or existing
     * @return the new/updated car is stored in the repository
     */
    public Car save(Car car) {
        if (car.getId() != null) {
            return repository.findById(car.getId())
                    .map(carToBeUpdated -> {
                        carToBeUpdated.setDetails(car.getDetails());
                        carToBeUpdated.setLocation(car.getLocation());
                        return repository.save(carToBeUpdated);
                    }).orElseThrow(CarNotFoundException::new);
        }

        return repository.save(car);
    }

    /**
     * Deletes a given car by ID
     * @param id the ID number of the car to delete
     */
    public void delete(Long id) {
        /**
         * TODO: Find the car by ID from the `repository` if it exists.
         *   If it does not exist, throw a CarNotFoundException
         */
        Optional<Car> optionalCar = repository.findById(id);
        if (optionalCar.isEmpty())
            throw new CarNotFoundException("Car with id " + id + " was not found!");


        /**
         * TODO: Delete the car from the repository.
         */
        repository.delete(optionalCar.get());

    }
}
