package com.example;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.sql.PreparedStatement;
import java.util.List;
import java.util.Objects;

@RestController
public class VehicleController {
    private final JdbcTemplate jdbcTemplate;

    public VehicleController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping("/vehicles")
    public ResponseEntity<?> getVehicles() {
        final List<Vehicle> vehicles = this.jdbcTemplate.query("SELECT id, name FROM vehicle ORDER BY id", (rs, i) -> new Vehicle(rs.getInt("id"), rs.getString("name")));
        return ResponseEntity.ok(vehicles);
    }

    @PostMapping(path = "/vehicles")
    public ResponseEntity<?> postVehicles(@RequestBody Vehicle vehicle) {
        final KeyHolder keyHolder = new GeneratedKeyHolder();
        this.jdbcTemplate.update(connection -> {
            final PreparedStatement statement = connection.prepareStatement("INSERT INTO vehicle(name) VALUES (?)", new String[]{"id"});
            statement.setString(1, vehicle.getName());
            return statement;
        }, keyHolder);
        vehicle.setId(Objects.requireNonNull(keyHolder.getKey()).intValue());
        return ResponseEntity.status(HttpStatus.CREATED).body(vehicle);
    }

    @DeleteMapping(path = "/vehicles/{id}")
    public ResponseEntity<?> deleteVehicle(@PathVariable("id") Integer id) {
        this.jdbcTemplate.update("DELETE FROM vehicle WHERE id = ?", id);
        return ResponseEntity.noContent().build();
    }

    static class Vehicle {

        public Vehicle(Integer id, String name) {
            this.id = id;
            this.name = name;
        }

        private Integer id;

        private String name;

        public Integer getId() {
            return id;
        }

        public void setId(Integer id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }
}
