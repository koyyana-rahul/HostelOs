import { useCallback, useEffect, useMemo, useState } from "react";
import { AlertCircle, Building2, MapPin, LoaderCircle, X } from "lucide-react";
import { useTranslation } from "react-i18next";
import { useForm } from "react-hook-form";
import { yupResolver } from "@hookform/resolvers/yup";
import * as yup from "yup";

import { Button } from "../../../components/ui/Button.jsx";
import { Modal } from "../../../components/ui/Modal.jsx";

import {
  useCreatePropertyMutation,
  useUpdatePropertyMutation,
} from "../../organizations/api/propertyApi.js";

import PropertyLocationPicker from "./PropertyLocationPicker.jsx";

const inputClassName =
  "mt-2 h-11 w-full rounded-lg border border-line bg-surface px-3 text-sm text-ink transition-colors placeholder:text-muted/70 hover:border-brand/30 focus:border-brand focus:outline-none focus:ring-2 focus:ring-brand/15 disabled:cursor-not-allowed disabled:bg-canvas disabled:text-muted";

const labelClassName = "block text-sm font-medium text-ink";

const sectionClassName =
  "space-y-4 rounded-xl border border-line bg-surface p-4 sm:p-5";

const countryOptions = [
  { value: "IN", label: "India" },
  { value: "US", label: "United States" },
  { value: "GB", label: "United Kingdom" },
  { value: "CA", label: "Canada" },
  { value: "AU", label: "Australia" },
  { value: "DE", label: "Germany" },
  { value: "FR", label: "France" },
  { value: "JP", label: "Japan" },
  { value: "SG", label: "Singapore" },
  { value: "AE", label: "United Arab Emirates" },
];

const countryStateMap = {
  IN: [
    "Andhra Pradesh",
    "Arunachal Pradesh",
    "Assam",
    "Bihar",
    "Chhattisgarh",
    "Goa",
    "Gujarat",
    "Haryana",
    "Himachal Pradesh",
    "Jharkhand",
    "Karnataka",
    "Kerala",
    "Madhya Pradesh",
    "Maharashtra",
    "Manipur",
    "Meghalaya",
    "Mizoram",
    "Nagaland",
    "Odisha",
    "Punjab",
    "Rajasthan",
    "Sikkim",
    "Tamil Nadu",
    "Telangana",
    "Tripura",
    "Uttar Pradesh",
    "Uttarakhand",
    "West Bengal",
  ],
  US: [
    "Alabama",
    "Alaska",
    "Arizona",
    "California",
    "Colorado",
    "Florida",
    "Georgia",
    "Illinois",
    "Massachusetts",
    "Michigan",
    "New Jersey",
    "New York",
    "North Carolina",
    "Ohio",
    "Pennsylvania",
    "Texas",
    "Virginia",
    "Washington",
  ],
  GB: ["England", "Scotland", "Wales", "Northern Ireland"],
  CA: [
    "Alberta",
    "British Columbia",
    "Manitoba",
    "New Brunswick",
    "Newfoundland and Labrador",
    "Nova Scotia",
    "Ontario",
    "Quebec",
    "Saskatchewan",
  ],
  AU: [
    "Australian Capital Territory",
    "New South Wales",
    "Northern Territory",
    "Queensland",
    "South Australia",
    "Tasmania",
    "Victoria",
    "Western Australia",
  ],
  DE: [
    "Baden-Württemberg",
    "Bavaria",
    "Berlin",
    "Brandenburg",
    "Bremen",
    "Hamburg",
    "Hesse",
    "Lower Saxony",
    "North Rhine-Westphalia",
    "Saxony",
  ],
  FR: [
    "Auvergne-Rhône-Alpes",
    "Bourgogne-Franche-Comté",
    "Bretagne",
    "Centre-Val de Loire",
    "Grand Est",
    "Hauts-de-France",
    "Île-de-France",
    "Normandie",
    "Nouvelle-Aquitaine",
    "Occitanie",
    "Pays de la Loire",
    "Provence-Alpes-Côte d'Azur",
  ],
  JP: [
    "Hokkaido",
    "Aomori",
    "Iwate",
    "Miyagi",
    "Akita",
    "Yamagata",
    "Fukushima",
    "Tokyo",
    "Osaka",
    "Kyoto",
  ],
  SG: ["Central", "North", "North-East", "East", "West"],
  AE: [
    "Abu Dhabi",
    "Ajman",
    "Dubai",
    "Fujairah",
    "Ras Al Khaimah",
    "Sharjah",
    "Umm Al Quwain",
  ],
};

const getStatesForCountry = (countryCode) => countryStateMap[countryCode] || [];

const toNullableNumber = (value) => {
  if (value === "" || value === null || value === undefined) {
    return null;
  }

  const number = Number(value);
  return Number.isFinite(number) ? number : null;
};

function SectionHeading({ icon: Icon, title, description }) {
  return (
    <div className="flex items-start gap-3">
      <div className="flex size-10 shrink-0 items-center justify-center rounded-lg bg-brand/10 text-brand">
        <Icon className="size-5" aria-hidden="true" />
      </div>

      <div className="min-w-0">
        <h3 className="text-sm font-semibold text-ink">{title}</h3>
        {description ? (
          <p className="mt-1 text-xs leading-5 text-muted">{description}</p>
        ) : null}
      </div>
    </div>
  );
}

function FieldError({ message }) {
  if (!message) return null;

  return (
    <p role="alert" className="mt-1.5 text-xs text-danger">
      {message}
    </p>
  );
}

export function PropertyFormModal({
  open,
  organizationId,
  property = null,
  onClose,
}) {
  const { t } = useTranslation();

  const [createProperty, { isLoading: isCreating }] =
    useCreatePropertyMutation();

  const [updateProperty, { isLoading: isUpdating }] =
    useUpdatePropertyMutation();

  const [requestError, setRequestError] = useState("");

  const isEditing = Boolean(property);
  const isLoading = isCreating || isUpdating;

  const defaultValues = useMemo(
    () => ({
      name: property?.name || "",
      code: property?.code || "",
      country: property?.country || "IN",
      state: property?.state || "",
      city: property?.city || "",
      addressLine1: property?.addressLine1 || "",
      addressLine2: property?.addressLine2 || "",
      postalCode: property?.postalCode || "",
      timezone:
        property?.timezone ||
        (property?.country === "IN" || !property?.country
          ? "Asia/Kolkata"
          : ""),
      latitude: toNullableNumber(property?.latitude),
      longitude: toNullableNumber(property?.longitude),
    }),
    [property],
  );

  const schema = useMemo(
    () =>
      yup.object({
        name: yup
          .string()
          .trim()
          .required(
            t("validation.required", {
              defaultValue: "This field is required.",
            }),
          )
          .max(120, "Property name cannot exceed 120 characters."),

        code: isEditing
          ? yup.string().notRequired()
          : yup
              .string()
              .trim()
              .required("Property code is required.")
              .min(2, "Property code must contain at least 2 characters.")
              .max(30, "Property code cannot exceed 30 characters.")
              .matches(
                /^[A-Za-z0-9_-]{2,30}$/,
                "Use only letters, numbers, hyphens and underscores.",
              ),

        country: yup.string().required("Please select a country."),

        state: yup
          .string()
          .trim()
          .required("Please select or enter a state / province.")
          .max(100, "State cannot exceed 100 characters."),

        city: yup
          .string()
          .trim()
          .required("City is required.")
          .max(100, "City cannot exceed 100 characters."),

        addressLine1: yup
          .string()
          .trim()
          .max(200, "Street address cannot exceed 200 characters."),

        addressLine2: yup
          .string()
          .trim()
          .max(200, "Additional address cannot exceed 200 characters."),

        postalCode: yup
          .string()
          .trim()
          .max(20, "Postal code cannot exceed 20 characters."),

        timezone: yup
          .string()
          .trim()
          .required("Time zone is required.")
          .max(50, "Time zone cannot exceed 50 characters."),

        latitude: yup
          .number()
          .transform((value, originalValue) =>
            originalValue === "" || originalValue == null ? null : value,
          )
          .nullable()
          .notRequired()
          .typeError("Latitude must be a valid number.")
          .min(-90, "Latitude cannot be below -90.")
          .max(90, "Latitude cannot exceed 90."),

        longitude: yup
          .number()
          .transform((value, originalValue) =>
            originalValue === "" || originalValue == null ? null : value,
          )
          .nullable()
          .notRequired()
          .typeError("Longitude must be a valid number.")
          .min(-180, "Longitude cannot be below -180.")
          .max(180, "Longitude cannot exceed 180."),
      }),
    [t, isEditing],
  );

  const form = useForm({
    resolver: yupResolver(schema),
    defaultValues,
    mode: "onBlur",
  });

  const {
    register,
    handleSubmit,
    reset,
    watch,
    setValue,
    formState: { errors },
  } = form;

  const selectedCountry = watch("country");
  const latitude = watch("latitude");
  const longitude = watch("longitude");

  const states = useMemo(
    () => getStatesForCountry(selectedCountry),
    [selectedCountry],
  );

  // Reset the form when opening a new property or editing another one.
  useEffect(() => {
    if (open) {
      reset(defaultValues);
      setRequestError("");
    }
  }, [open, defaultValues, reset]);

  const handleCountryChange = useCallback(
    (event) => {
      const nextCountry = event.target.value;

      setValue("country", nextCountry, {
        shouldDirty: true,
        shouldValidate: true,
      });

      // State belongs to the previous country and must be cleared.
      setValue("state", "", {
        shouldDirty: true,
        shouldValidate: true,
      });

      if (nextCountry === "IN") {
        setValue("timezone", "Asia/Kolkata", {
          shouldDirty: true,
        });
      } else {
        setValue("timezone", "", {
          shouldDirty: true,
        });
      }
    },
    [setValue],
  );

  const handleMapPositionChange = useCallback(
    (lat, lng) => {
      setValue("latitude", lat, {
        shouldDirty: true,
        shouldValidate: true,
      });

      setValue("longitude", lng, {
        shouldDirty: true,
        shouldValidate: true,
      });
    },
    [setValue],
  );

  const handleSearchResult = useCallback((result) => {
    // Do not overwrite manually entered address information.
    // The administrator can review the search result on the map.
    if (!result) return;
  }, []);

  const onFinish = async (values) => {
    if (!organizationId) {
      setRequestError("Organization information is missing.");
      return;
    }

    setRequestError("");

    const payload = {
      name: values.name.trim(),
      addressLine1: values.addressLine1?.trim() || "",
      addressLine2: values.addressLine2?.trim() || "",
      city: values.city.trim(),
      state: values.state.trim(),
      postalCode: values.postalCode?.trim() || "",
      country: values.country,
      timezone: values.timezone.trim(),
      latitude: toNullableNumber(values.latitude),
      longitude: toNullableNumber(values.longitude),
    };

    try {
      if (isEditing) {
        await updateProperty({
          organizationId,
          propertyId: property.id,
          ...payload,
        }).unwrap();
      } else {
        await createProperty({
          organizationId,
          code: values.code.trim(),
          ...payload,
        }).unwrap();
      }

      onClose?.();
    } catch (error) {
      setRequestError(
        error?.data?.message ||
          error?.data?.detail ||
          error?.data?.error ||
          "Unable to save the property. Please check your details and try again.",
      );
    }
  };

  const nameRegistration = register("name");
  const codeRegistration = register("code");
  const countryRegistration = register("country");
  const stateRegistration = register("state");

  return (
    <Modal
      open={open}
      title={isEditing ? "Edit Property" : "Add New Property"}
      description={
        isEditing
          ? "Update the property information and location."
          : "Register a hostel or PG property under your organization."
      }
      onClose={isLoading ? undefined : onClose}
    >
      <form
        onSubmit={handleSubmit(onFinish)}
        noValidate
        className="flex max-h-[75vh] flex-col"
      >
        <div className="min-h-0 space-y-5 overflow-y-auto px-1 pb-5">
          {/* API Error */}
          {requestError ? (
            <div
              role="alert"
              className="flex items-start gap-3 rounded-xl border border-danger/20 bg-danger-soft/60 p-3"
            >
              <AlertCircle
                className="mt-0.5 size-4 shrink-0 text-danger"
                aria-hidden="true"
              />

              <p className="flex-1 text-sm text-ink">{requestError}</p>

              <button
                type="button"
                onClick={() => setRequestError("")}
                className="text-muted hover:text-ink"
                aria-label="Dismiss error"
              >
                <X className="size-4" />
              </button>
            </div>
          ) : null}

          {/* SECTION 1: Property Information */}
          <section className={sectionClassName}>
            <SectionHeading
              icon={Building2}
              title="Property Information"
              description="Enter the basic details of your hostel or PG."
            />

            <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
              <label className="block">
                <span className={labelClassName}>
                  Property Name <span className="text-danger">*</span>
                </span>

                <input
                  {...nameRegistration}
                  onChange={(event) => {
                    nameRegistration.onChange(event);
                    setRequestError("");
                  }}
                  disabled={isLoading}
                  maxLength={120}
                  className={inputClassName}
                  placeholder="e.g. Green Valley Residency"
                />

                <FieldError message={errors.name?.message} />
              </label>

              {!isEditing ? (
                <label className="block">
                  <span className={labelClassName}>
                    Property Code <span className="text-danger">*</span>
                  </span>

                  <input
                    {...codeRegistration}
                    onChange={(event) => {
                      codeRegistration.onChange(event);
                      setRequestError("");
                    }}
                    disabled={isLoading}
                    maxLength={30}
                    className={inputClassName}
                    placeholder="e.g. GVH-001"
                  />

                  <FieldError message={errors.code?.message} />
                </label>
              ) : null}
            </div>
          </section>

          {/* SECTION 2: Address */}
          <section className={sectionClassName}>
            <SectionHeading
              icon={MapPin}
              title="Property Address"
              description="Provide the registered address of this property."
            />

            <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
              <label className="block">
                <span className={labelClassName}>
                  Country <span className="text-danger">*</span>
                </span>

                <select
                  {...countryRegistration}
                  onChange={(event) => {
                    countryRegistration.onChange(event);
                    handleCountryChange(event);
                  }}
                  disabled={isLoading}
                  className={inputClassName}
                >
                  <option value="">Select country</option>

                  {countryOptions.map((option) => (
                    <option key={option.value} value={option.value}>
                      {option.label}
                    </option>
                  ))}
                </select>

                <FieldError message={errors.country?.message} />
              </label>

              <label className="block">
                <span className={labelClassName}>
                  State / Province <span className="text-danger">*</span>
                </span>

                {states.length > 0 ? (
                  <select
                    {...stateRegistration}
                    disabled={isLoading || !selectedCountry}
                    className={inputClassName}
                  >
                    <option value="">Select state / province</option>

                    {states.map((item) => (
                      <option key={item} value={item}>
                        {item}
                      </option>
                    ))}
                  </select>
                ) : (
                  <input
                    {...stateRegistration}
                    disabled={isLoading || !selectedCountry}
                    maxLength={100}
                    className={inputClassName}
                    placeholder="Enter state or province"
                  />
                )}

                <FieldError message={errors.state?.message} />
              </label>

              <label className="block">
                <span className={labelClassName}>
                  City <span className="text-danger">*</span>
                </span>

                <input
                  {...register("city")}
                  disabled={isLoading}
                  maxLength={100}
                  className={inputClassName}
                  placeholder="Enter city"
                />

                <FieldError message={errors.city?.message} />
              </label>

              <label className="block">
                <span className={labelClassName}>Postal Code</span>

                <input
                  {...register("postalCode")}
                  disabled={isLoading}
                  maxLength={20}
                  className={inputClassName}
                  placeholder="Enter postal or ZIP code"
                />

                <FieldError message={errors.postalCode?.message} />
              </label>

              <label className="block sm:col-span-2">
                <span className={labelClassName}>Street Address</span>

                <input
                  {...register("addressLine1")}
                  disabled={isLoading}
                  maxLength={200}
                  className={inputClassName}
                  placeholder="Building number, street name"
                />

                <FieldError message={errors.addressLine1?.message} />
              </label>

              <label className="block sm:col-span-2">
                <span className={labelClassName}>
                  Area / Locality / Landmark
                </span>

                <input
                  {...register("addressLine2")}
                  disabled={isLoading}
                  maxLength={200}
                  className={inputClassName}
                  placeholder="Area, locality or nearby landmark"
                />

                <FieldError message={errors.addressLine2?.message} />
              </label>

              <label className="block sm:col-span-2">
                <span className={labelClassName}>
                  Time Zone <span className="text-danger">*</span>
                </span>

                <input
                  {...register("timezone")}
                  disabled={isLoading}
                  maxLength={50}
                  list="property-timezones"
                  className={inputClassName}
                  placeholder="e.g. Asia/Kolkata"
                />

                <datalist id="property-timezones">
                  <option value="Asia/Kolkata" />
                  <option value="Asia/Dubai" />
                  <option value="Asia/Singapore" />
                  <option value="Europe/London" />
                  <option value="Europe/Berlin" />
                  <option value="America/New_York" />
                  <option value="America/Los_Angeles" />
                  <option value="Australia/Sydney" />
                  <option value="Asia/Tokyo" />
                </datalist>

                <p className="mt-1.5 text-xs text-muted">
                  Use an IANA time zone identifier for accurate local dates and
                  times.
                </p>

                <FieldError message={errors.timezone?.message} />
              </label>
            </div>
          </section>

          {/* SECTION 3: Map Location */}
          <section className={sectionClassName}>
            <SectionHeading
              icon={MapPin}
              title="Property Location"
              description="Find your property on the map and select its exact geographic position."
            />

            <PropertyLocationPicker
              latitude={latitude}
              longitude={longitude}
              onPositionChange={handleMapPositionChange}
              onSearchResult={handleSearchResult}
              disabled={isLoading}
            />

            <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
              <label className="block">
                <span className={labelClassName}>Latitude</span>

                <input
                  type="number"
                  step="any"
                  {...register("latitude", {
                    setValueAs: toNullableNumber,
                  })}
                  disabled={isLoading}
                  className={inputClassName}
                  placeholder="Select on map or enter manually"
                />

                <FieldError message={errors.latitude?.message} />
              </label>

              <label className="block">
                <span className={labelClassName}>Longitude</span>

                <input
                  type="number"
                  step="any"
                  {...register("longitude", {
                    setValueAs: toNullableNumber,
                  })}
                  disabled={isLoading}
                  className={inputClassName}
                  placeholder="Select on map or enter manually"
                />

                <FieldError message={errors.longitude?.message} />
              </label>
            </div>

            <p className="text-xs leading-5 text-muted">
              Coordinates are optional. Selecting a location on the map
              automatically fills both values. You can also enter them manually.
            </p>
          </section>
        </div>

        {/* Footer */}
        <div className="flex shrink-0 flex-col-reverse gap-2 border-t border-line bg-surface pt-4 sm:flex-row sm:justify-end">
          <Button
            type="button"
            variant="secondary"
            onClick={onClose}
            disabled={isLoading}
          >
            Cancel
          </Button>

          <Button type="submit" disabled={isLoading || !organizationId}>
            {isLoading ? (
              <>
                <LoaderCircle
                  className="mr-2 size-4 animate-spin"
                  aria-hidden="true"
                />
                Saving...
              </>
            ) : isEditing ? (
              "Save Changes"
            ) : (
              "Create Property"
            )}
          </Button>
        </div>
      </form>
    </Modal>
  );
}

export default PropertyFormModal;
