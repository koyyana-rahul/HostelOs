import i18n from 'i18next'
import { initReactI18next } from 'react-i18next'
import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import App from './app/App.jsx'
import './styles/globals.css'

const en = {
  translation: {
    app: { name: 'HostelOS' },
    fields: {
      name: 'Property Name',
      code: 'Property Code',
      country: 'Country',
      state: 'State / Province',
      city: 'City',
      address_line_1: 'Street Address',
      address_line_2: 'Area / Locality / Landmark',
      postal_code: 'Postal Code',
      timezone: 'Time Zone',
      latitude: 'Latitude',
      longitude: 'Longitude',
      select_country: 'Select a country',
      select_state: 'Select a state',
      select_timezone: 'Select a time zone',
    },
    placeholders: {
      name: 'Green Valley Hostel',
      code: 'GVH-01',
      city: 'Bengaluru',
      address_line_1: 'Plot 42, North Campus Road',
      address_line_2: 'Near University Gate',
      postal_code: '560001',
      timezone: 'Asia/Kolkata',
      latitude: '12.9719',
      longitude: '77.5937',
    },
    info: {
      lat_lon_note: '(decimal degrees)',
      map_hint: 'Click on the map or drag the marker to set the property location.',
      selected_location: 'Selected location',
    },
    validation: {
      required: 'This field is required.',
      max_length: 'Must not exceed {{max}} characters.',
      min_length: 'Must be at least {{min}} characters.',
      min_value: 'Must be at least {{min}}.',
      max_value: 'Must not exceed {{max}}.',
      pattern_invalid: 'Invalid format.',
    },
    action: {
      cancel: 'Cancel',
      save_changes: 'Save Changes',
      create_property: 'Create Property',
      search: 'Search',
    },
    map: {
      attribution: '© OpenStreetMap contributors',
      search_placeholder: 'Search address...',
      search: 'Search',
      locate: 'Locate on map',
    },
  },
}

i18n
  .use(initReactI18next)
  .init({
    fallbackLng: 'en',
    interpolation: { escapeValue: false, prefix: '{{', suffix: '}}' },
    resources: { en },
  })

createRoot(document.getElementById('root')).render(
  <StrictMode>
    <App />
  </StrictMode>,
)