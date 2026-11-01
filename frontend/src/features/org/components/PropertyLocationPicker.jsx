import { useCallback, useEffect, useRef, useState } from "react";
import {
  MapContainer,
  TileLayer,
  Marker,
  useMapEvents,
  useMap,
} from "react-leaflet";
import { Search, MapPin, LoaderCircle, X } from "lucide-react";
import L from "leaflet";
import { useTranslation } from "react-i18next";
import { Button } from "../../../components/ui/Button.jsx";

import "leaflet/dist/leaflet.css";

// --------------------------------------------------
// Leaflet marker configuration
// --------------------------------------------------

delete L.Icon.Default.prototype._getIconUrl;

L.Icon.Default.mergeOptions({
  iconRetinaUrl:
    "https://unpkg.com/leaflet@1.9.4/dist/images/marker-icon-2x.png",
  iconUrl: "https://unpkg.com/leaflet@1.9.4/dist/images/marker-icon.png",
  shadowUrl: "https://unpkg.com/leaflet@1.9.4/dist/images/marker-shadow.png",
});

// --------------------------------------------------
// Constants
// --------------------------------------------------

const DEFAULT_MAP_CENTER = [20, 0];
const DEFAULT_MAP_ZOOM = 2;
const SELECTED_MAP_ZOOM = 15;

const inputClassName =
  "h-11 w-full rounded-lg border border-line bg-surface px-3 pl-10 text-sm text-ink transition-colors placeholder:text-muted/70 hover:border-brand/30 focus:border-brand focus:outline-none focus:ring-2 focus:ring-brand/15 disabled:cursor-not-allowed disabled:bg-canvas disabled:text-muted";

// --------------------------------------------------
// Coordinate validation
// --------------------------------------------------

function isValidCoordinate(latitude, longitude) {
  if (
    latitude === null ||
    latitude === undefined ||
    longitude === null ||
    longitude === undefined ||
    latitude === "" ||
    longitude === ""
  ) {
    return false;
  }

  const lat = Number(latitude);
  const lng = Number(longitude);

  return (
    Number.isFinite(lat) &&
    Number.isFinite(lng) &&
    lat >= -90 &&
    lat <= 90 &&
    lng >= -180 &&
    lng <= 180
  );
}

// --------------------------------------------------
// Map click handler
// --------------------------------------------------

function MapClickHandler({ onPositionChange, disabled }) {
  useMapEvents({
    click(event) {
      if (disabled) return;

      onPositionChange(event.latlng.lat, event.latlng.lng);
    },
  });

  return null;
}

// --------------------------------------------------
// Map center handler
// --------------------------------------------------

function MapCenterHandler({ center }) {
  const map = useMap();

  useEffect(() => {
    if (!center) return;

    map.flyTo(
      [center.lat, center.lng],
      Math.max(map.getZoom(), SELECTED_MAP_ZOOM),
      { duration: 0.6 },
    );
  }, [center, map]);

  return null;
}

// --------------------------------------------------
// Main component
// --------------------------------------------------

export function PropertyLocationPicker({
  latitude,
  longitude,
  onPositionChange,
  onSearchResult,
  disabled = false,
}) {
  const { t } = useTranslation();

  const [searchQuery, setSearchQuery] = useState("");
  const [isSearching, setIsSearching] = useState(false);
  const [searchError, setSearchError] = useState("");
  const [center, setCenter] = useState(null);

  const searchControllerRef = useRef(null);

  const hasSelectedLocation = isValidCoordinate(latitude, longitude);

  const position = hasSelectedLocation
    ? {
        lat: Number(latitude),
        lng: Number(longitude),
      }
    : null;

  // ------------------------------------------------
  // Synchronize coordinates with parent form
  // ------------------------------------------------

  useEffect(() => {
    if (hasSelectedLocation) {
      setCenter({
        lat: Number(latitude),
        lng: Number(longitude),
      });
    } else {
      setCenter(null);
    }
  }, [latitude, longitude, hasSelectedLocation]);

  // ------------------------------------------------
  // Abort pending search on unmount
  // ------------------------------------------------

  useEffect(() => {
    return () => {
      searchControllerRef.current?.abort();
    };
  }, []);

  // ------------------------------------------------
  // Update selected position
  // ------------------------------------------------

  const handlePositionChange = useCallback(
    (lat, lng) => {
      const nextLatitude = Number(lat);
      const nextLongitude = Number(lng);

      if (
        !Number.isFinite(nextLatitude) ||
        !Number.isFinite(nextLongitude) ||
        nextLatitude < -90 ||
        nextLatitude > 90 ||
        nextLongitude < -180 ||
        nextLongitude > 180
      ) {
        return;
      }

      onPositionChange?.(nextLatitude, nextLongitude);

      setCenter({
        lat: nextLatitude,
        lng: nextLongitude,
      });

      setSearchError("");
    },
    [onPositionChange],
  );

  // ------------------------------------------------
  // Search location
  // ------------------------------------------------

  const handleSearch = useCallback(
    async (event) => {
      event.preventDefault();

      const query = searchQuery.trim();

      if (!query || disabled) return;

      // Cancel previous search.
      searchControllerRef.current?.abort();

      const controller = new AbortController();

      searchControllerRef.current = controller;

      setIsSearching(true);
      setSearchError("");

      try {
        const params = new URLSearchParams({
          format: "jsonv2",
          limit: "1",
          q: query,
          addressdetails: "1",
        });

        const response = await fetch(
          `https://nominatim.openstreetmap.org/search?${params.toString()}`,
          {
            method: "GET",
            headers: {
              Accept: "application/json",
              "Accept-Language": "en",
            },
            signal: controller.signal,
          },
        );

        if (!response.ok) {
          throw new Error("Location search failed");
        }

        const results = await response.json();

        if (!Array.isArray(results) || results.length === 0) {
          setSearchError(
            t("map.no_results", {
              defaultValue: "No matching location was found.",
            }),
          );
          return;
        }

        const result = results[0];

        const lat = Number(result.lat);
        const lng = Number(result.lon);

        if (!isValidCoordinate(lat, lng)) {
          throw new Error("Invalid coordinates received");
        }

        handlePositionChange(lat, lng);

        onSearchResult?.({
          lat,
          lng,
          displayName: result.display_name || "",
          address: result.address || {},
        });
      } catch (error) {
        if (error.name === "AbortError") return;

        setSearchError(
          t("map.search_error", {
            defaultValue:
              "Unable to search for this location. Please try again.",
          }),
        );
      } finally {
        if (!controller.signal.aborted) {
          setIsSearching(false);
        }
      }
    },
    [searchQuery, disabled, handlePositionChange, onSearchResult, t],
  );

  // ------------------------------------------------
  // Clear selected location
  // ------------------------------------------------

  const clearLocation = useCallback(() => {
    onPositionChange?.(null, null);

    setCenter(null);
    setSearchError("");
  }, [onPositionChange]);

  // ------------------------------------------------
  // Render
  // ------------------------------------------------

  return (
    <section className="space-y-4">
      {/* -------------------------------------------
          Address search
      -------------------------------------------- */}

      <div className="space-y-2">
        <label
          htmlFor="property-location-search"
          className="block text-sm font-medium text-ink"
        >
          {t("map.search_label", {
            defaultValue: "Search for a property location",
          })}
        </label>

        {/* IMPORTANT: Use div, NOT form.
            The parent PropertyFormModal already has a form. */}

        <div className="flex flex-col gap-2 sm:flex-row">
          <div className="relative min-w-0 flex-1">
            <Search
              className="pointer-events-none absolute left-3 top-1/2 size-4 -translate-y-1/2 text-muted"
              aria-hidden="true"
            />

            <input
              id="property-location-search"
              type="search"
              value={searchQuery}
              onChange={(event) => {
                setSearchQuery(event.target.value);
                setSearchError("");
              }}
              onKeyDown={(event) => {
                if (event.key === "Enter") {
                  event.preventDefault();
                  handleSearch(event);
                }
              }}
              placeholder={t("map.search_placeholder", {
                defaultValue: "Search address, area, city or landmark",
              })}
              className={inputClassName}
              disabled={disabled || isSearching}
              autoComplete="off"
            />
          </div>

          <Button
            type="button"
            size="sm"
            disabled={disabled || isSearching || !searchQuery.trim()}
            onClick={handleSearch}
          >
            {isSearching ? (
              <>
                <LoaderCircle
                  className="mr-2 size-4 animate-spin"
                  aria-hidden="true"
                />
                Searching...
              </>
            ) : (
              <>
                <Search className="mr-2 size-4" aria-hidden="true" />
                Search
              </>
            )}
          </Button>
        </div>

        {searchError ? (
          <p role="alert" className="text-sm text-danger">
            {searchError}
          </p>
        ) : null}
      </div>

      {/* -------------------------------------------
          Interactive map
      -------------------------------------------- */}

      <div className="overflow-hidden rounded-xl border border-line bg-surface">
        <div className="flex items-center justify-between gap-3 border-b border-line px-3 py-3">
          <div className="flex items-center gap-2">
            <MapPin className="size-4 text-brand" aria-hidden="true" />

            <span className="text-sm font-medium text-ink">
              {t("map.select_location", {
                defaultValue: "Select property location",
              })}
            </span>
          </div>

          {hasSelectedLocation ? (
            <button
              type="button"
              onClick={clearLocation}
              disabled={disabled}
              className="inline-flex items-center gap-1 text-xs font-medium text-muted transition-colors hover:text-danger disabled:cursor-not-allowed"
            >
              <X className="size-3.5" aria-hidden="true" />

              {t("map.clear_location", {
                defaultValue: "Clear",
              })}
            </button>
          ) : null}
        </div>

        <MapContainer
          center={position ? [position.lat, position.lng] : DEFAULT_MAP_CENTER}
          zoom={position ? SELECTED_MAP_ZOOM : DEFAULT_MAP_ZOOM}
          scrollWheelZoom
          zoomControl
          attributionControl
          className="z-0 h-[320px] w-full sm:h-[380px]"
        >
          <TileLayer
            url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
            attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
            maxZoom={19}
          />

          <MapCenterHandler center={center} />

          <MapClickHandler
            onPositionChange={handlePositionChange}
            disabled={disabled}
          />

          {position ? (
            <Marker
              position={[position.lat, position.lng]}
              draggable={!disabled}
              eventHandlers={{
                dragend(event) {
                  const markerPosition = event.target.getLatLng();

                  handlePositionChange(markerPosition.lat, markerPosition.lng);
                },
              }}
            />
          ) : null}
        </MapContainer>
      </div>

      {/* -------------------------------------------
          Selected coordinates
      -------------------------------------------- */}

      <div className="rounded-xl border border-line bg-canvas/50 p-3">
        <div className="mb-3 flex items-center justify-between gap-2">
          <span className="text-sm font-medium text-ink">
            {t("map.selected_location", {
              defaultValue: "Selected coordinates",
            })}
          </span>

          {hasSelectedLocation ? (
            <span className="rounded-full bg-success-soft px-2 py-1 text-xs font-medium text-success">
              {t("map.location_selected", {
                defaultValue: "Location selected",
              })}
            </span>
          ) : (
            <span className="rounded-full bg-surface px-2 py-1 text-xs text-muted">
              {t("map.location_not_selected", {
                defaultValue: "Not selected",
              })}
            </span>
          )}
        </div>

        <div className="grid grid-cols-2 gap-3">
          <div>
            <p className="mb-1 text-xs text-muted">Latitude</p>

            <p className="font-mono text-sm text-ink">
              {hasSelectedLocation ? position.lat.toFixed(6) : "—"}
            </p>
          </div>

          <div>
            <p className="mb-1 text-xs text-muted">Longitude</p>

            <p className="font-mono text-sm text-ink">
              {hasSelectedLocation ? position.lng.toFixed(6) : "—"}
            </p>
          </div>
        </div>
      </div>

      {/* -------------------------------------------
          Instructions
      -------------------------------------------- */}

      <p className="text-xs leading-5 text-muted">
        {t("map.map_hint", {
          defaultValue:
            "Search for an address, click on the map, or drag the marker to select the exact property location.",
        })}
      </p>
    </section>
  );
}

PropertyLocationPicker.displayName = "PropertyLocationPicker";

export default PropertyLocationPicker;
