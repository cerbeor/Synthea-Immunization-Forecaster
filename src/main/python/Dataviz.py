import os
import json
import pandas as pd
import plotly.express as px
import plotly.graph_objects as go

# Folder containing JSON files
json_folder_path = 'output/fhir/'

# Data storage
data = []

# Iterate over each file in the folder
for file_name in os.listdir(json_folder_path):
    if file_name.endswith('.json'):  # Check if the file is a JSON file
        file_path = os.path.join(json_folder_path, file_name)
        with open(file_path, 'r') as file:
            json_data = json.load(file)
            # Check for 'entry' and 'resourceType'
            if 'entry' in json_data:
                for entry in json_data['entry']:
                    if entry.get('resource', {}).get('resourceType') == 'Patient':
                        resource = entry['resource']
                        # Extract ANTIVAX_STATUS and STATE
                        antivax_status = None
                        state = None
                        for extension in resource.get('extension', []):
                            if extension['url'] == "http://synthetichealth.github.io/synthea/antivax":
                                antivax_status = extension.get('valueBoolean', None)
                            if extension['url'] == "http://hl7.org/fhir/StructureDefinition/patient-birthPlace":
                                state = extension.get('valueAddress', {}).get('state', None)
                        if antivax_status is not None and state is not None:
                            data.append({'STATE': state, 'ANTIVAX_STATUS': antivax_status})

# Convert to DataFrame
df = pd.DataFrame(data)

print(df.head())

# Define state name to abbreviation mapping 
state_name_to_abbreviation = {
    'Alabama': 'AL', 'Alaska': 'AK', 'Arizona': 'AZ', 'Arkansas': 'AR',
    'California': 'CA', 'Colorado': 'CO', 'Connecticut': 'CT', 'Delaware': 'DE',
    'Florida': 'FL', 'Georgia': 'GA', 'Hawaii': 'HI', 'Idaho': 'ID',
    'Illinois': 'IL', 'Indiana': 'IN', 'Iowa': 'IA', 'Kansas': 'KS',
    'Kentucky': 'KY', 'Louisiana': 'LA', 'Maine': 'ME', 'Maryland': 'MD',
    'Massachusetts': 'MA', 'Michigan': 'MI', 'Minnesota': 'MN', 'Mississippi': 'MS',
    'Missouri': 'MO', 'Montana': 'MT', 'Nebraska': 'NE', 'Nevada': 'NV',
    'New Hampshire': 'NH', 'New Jersey': 'NJ', 'New Mexico': 'NM', 'New York': 'NY',
    'North Carolina': 'NC', 'North Dakota': 'ND', 'Ohio': 'OH', 'Oklahoma': 'OK',
    'Oregon': 'OR', 'Pennsylvania': 'PA', 'Rhode Island': 'RI',
    'South Carolina': 'SC', 'South Dakota': 'SD', 'Tennessee': 'TN', 'Texas': 'TX',
    'Utah': 'UT', 'Vermont': 'VT', 'Virginia': 'VA', 'Washington': 'WA',
    'West Virginia': 'WV', 'Wisconsin': 'WI', 'Wyoming': 'WY'
}

# Ensure ANTIVAX_STATUS is a boolean and clean column names
df.columns = df.columns.str.strip()  # Remove any leading/trailing spaces in column names
df['ANTIVAX_STATUS'] = df['ANTIVAX_STATUS'].astype(bool)

# Map state names to abbreviations
df['STATE'] = df['STATE'].map(state_name_to_abbreviation)
df = df.dropna(subset=['STATE'])  # Drop rows where state mapping fails

# Calculate counts by state
state_counts = (
    df.groupby('STATE', as_index=False)  # Group by STATE
    .agg(
        total_people=('ANTIVAX_STATUS', 'size'),  # Count total entries per state
        antivax_count=('ANTIVAX_STATUS', 'sum')  # Sum of true (antivax) entries
    )
)

# Add antivax percentage column
state_counts['antivax_percentage'] = (
    state_counts['antivax_count'] / state_counts['total_people'] * 100
).fillna(0)

# Add missing states with zero counts to ensure all states are represented
all_states = set(['AL', 'AK', 'AZ', 'AR', 'CA', 'CO', 'CT', 'DE', 'FL', 'GA', 'HI', 'ID', 'IL',
                  'IN', 'IA', 'KS', 'KY', 'LA', 'ME', 'MD', 'MA', 'MI', 'MN', 'MS', 'MO', 'MT',
                  'NE', 'NV', 'NH', 'NJ', 'NM', 'NY', 'NC', 'ND', 'OH', 'OK', 'OR', 'PA', 'RI',
                  'SC', 'SD', 'TN', 'TX', 'UT', 'VT', 'VA', 'WA', 'WV', 'WI', 'WY'])
existing_states = set(state_counts['STATE'])
missing_states = all_states - existing_states
missing_states_df = pd.DataFrame({'STATE': list(missing_states), 'total_people': 0, 'antivax_count': 0, 'antivax_percentage': 0})
state_counts = pd.concat([state_counts, missing_states_df], ignore_index=True)

# Create choropleth map
fig = px.choropleth(
    state_counts,
    locations='STATE',
    locationmode='USA-states',
    color='antivax_percentage',
    color_continuous_scale='Blues',
    scope='usa',
    title='Antivax Individuals by State (Generated Population)',
    labels={
        'antivax_percentage': 'Antivax Percentage',
        'total_people' : 'Total People',
        'antivax_count' : 'Antivax Count'
    },
    hover_data={
        'total_people': ':,',
        'antivax_count': ':,',
        'antivax_percentage': ':.2f'
    }
)

# Improve map details
fig.update_geos(
    showcountries=False,
    showsubunits=True,  # This enables state boundaries
    subunitcolor="black",  # Color for state boundaries
    subunitwidth=1,  # Thickness of state boundaries
    showlakes=True,
    lakecolor="lightblue"  # Color for lakes
)

# Use state centroids for accurate placement
state_centroids = {
    'AL': {'lat': 32.806671, 'lon': -86.791130},
    'AK': {'lat': 61.370716, 'lon': -152.404419},
    'AZ': {'lat': 33.729759, 'lon': -111.431221},
    'AR': {'lat': 34.969704, 'lon': -92.373123},
    'CA': {'lat': 36.116203, 'lon': -119.681564},
    'CO': {'lat': 39.059811, 'lon': -105.311104},
    'CT': {'lat': 41.597782, 'lon': -72.755371},
    'DE': {'lat': 39.318523, 'lon': -75.507141},
    'FL': {'lat': 27.766279, 'lon': -81.686783},
    'GA': {'lat': 33.040619, 'lon': -83.643074},
    'HI': {'lat': 21.094318, 'lon': -157.498337},
    'ID': {'lat': 44.240459, 'lon': -114.478828},
    'IL': {'lat': 40.349457, 'lon': -88.986137},
    'IN': {'lat': 39.849426, 'lon': -86.258278},
    'IA': {'lat': 42.011539, 'lon': -93.210526},
    'KS': {'lat': 38.526600, 'lon': -96.726486},
    'KY': {'lat': 37.668140, 'lon': -84.670067},
    'LA': {'lat': 31.169546, 'lon': -91.867805},
    'ME': {'lat': 44.693947, 'lon': -69.381927},
    'MD': {'lat': 39.063946, 'lon': -76.802101},
    'MA': {'lat': 42.230171, 'lon': -71.530106},
    'MI': {'lat': 43.326618, 'lon': -84.536095},
    'MN': {'lat': 45.694454, 'lon': -93.900192},
    'MS': {'lat': 32.741646, 'lon': -89.678696},
    'MO': {'lat': 38.456085, 'lon': -92.288368},
    'MT': {'lat': 46.921925, 'lon': -110.454353},
    'NE': {'lat': 41.125370, 'lon': -98.268082},
    'NV': {'lat': 38.313515, 'lon': -117.055374},
    'NH': {'lat': 43.452492, 'lon': -71.563896},
    'NJ': {'lat': 40.298904, 'lon': -74.521011},
    'NM': {'lat': 34.840515, 'lon': -106.248482},
    'NY': {'lat': 42.165726, 'lon': -74.948051},
    'NC': {'lat': 35.630066, 'lon': -79.806419},
    'ND': {'lat': 47.528912, 'lon': -99.784012},
    'OH': {'lat': 40.388783, 'lon': -82.764915},
    'OK': {'lat': 35.565342, 'lon': -96.928917},
    'OR': {'lat': 44.572021, 'lon': -122.070938},
    'PA': {'lat': 40.590752, 'lon': -77.209755},
    'RI': {'lat': 41.680893, 'lon': -71.511780},
    'SC': {'lat': 33.856892, 'lon': -80.945007},
    'SD': {'lat': 44.299782, 'lon': -99.438828},
    'TN': {'lat': 35.747845, 'lon': -86.692345},
    'TX': {'lat': 31.054487, 'lon': -97.563461},
    'UT': {'lat': 40.150032, 'lon': -111.862434},
    'VT': {'lat': 44.045876, 'lon': -72.710686},
    'VA': {'lat': 37.769337, 'lon': -78.169968},
    'WA': {'lat': 47.400902, 'lon': -121.490494},
    'WV': {'lat': 38.491226, 'lon': -80.954453},
    'WI': {'lat': 44.268543, 'lon': -89.616508},
    'WY': {'lat': 42.755966, 'lon': -107.302490},
}

for idx, row in state_counts.iterrows():
    state = row['STATE']
    if state in state_centroids:
        hover_text = (
            f"State: {state}<br>"
            f"Antivax Count: {row['antivax_count']:,}<br>"
            f"Total People: {row['total_people']:,}<br>"
            f"Antivax Percentage: {row['antivax_percentage']:.2f}%"
        )
        fig.add_trace(go.Scattergeo(
            lat=[state_centroids[state]['lat']],
            lon=[state_centroids[state]['lon']],
            text=f"{row['antivax_count']}/{row['total_people']}",  # Text displayed on the map
            hovertext=hover_text,  # Use hovertext for detailed hover information
            mode='text',
            textfont=dict(
                size=10,
                color='black' if row['antivax_count'] > 0 else 'gray'
            ),
            hoverinfo='text',  # Use the text and hovertext for hover display
            showlegend=False
        ))

# Calculate totals
total_people = state_counts['total_people'].sum()
total_antivax = state_counts['antivax_count'].sum()
antivax_percentage = (total_antivax / total_people) * 100 if total_people > 0 else 0

# Add better color bar and layout customization
fig.update_layout(
    title=(
        f"<b>Antivax Individuals by State (Generated Population)</b><br>"
        f"<br>Total People: {total_people:,} | Total Antivax: {total_antivax:,} "
        f"({antivax_percentage:.2f}%)"
    ),
    geo=dict(
        showocean=True,
        oceancolor='lightblue'
    ),
    margin=dict(l=50, r=50, t=50, b=50),
    coloraxis_colorbar=dict(
        title='% Antivax Individuals',
        tickvals=[0, 25, 50, 75, 100],
        ticktext=['0%', '25%', '50%', '75%', '100%']
    )
)

# Show the map
fig.show()
