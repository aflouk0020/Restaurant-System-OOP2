package ie.tus.oop2.restaurant.service;

import ie.tus.oop2.restaurant.ui.model.AppSettings;

public interface SettingsService {
    AppSettings load();
    void save(AppSettings settings);
    AppSettings defaults();
}