import os
import json
import pandas as pd
import plotly.express as px
import plotly.graph_objects as go


def process_patient_data(json_folder_path):
    """
    Process JSON files in the given folder, extract patient data related to
    hesitancy status, return both a 'raw' dataframe and a grouped 'state_counts' dataframe.
    """
    data = []

    # 1. Read JSON files and extract data
    for file_name in os.listdir(json_folder_path):
        if file_name.endswith('.json'):
            file_path = os.path.join(json_folder_path, file_name)
            with open(file_path, 'r') as file:
                json_data = json.load(file)

                # Check for 'entry' in the JSON
                if 'entry' in json_data:
                    for entry in json_data['entry']:
                        if entry.get('resource', {}).get('resourceType') == 'Patient':
                            resource = entry['resource']

                            # Extract 'ANTIVAX_STATUS' and 'STATE'
                            hesitantPatient_status = None
                            state = None
                            for extension in resource.get('extension', []):
                                if extension['url'] == "http://synthetichealth.github.io/synthea/hesitantPatient":
                                    hesitantPatient_status = extension.get('valueBoolean', None)

                            address_list = resource.get('address', [])
                            if len(address_list) > 0:
                                state = address_list[0].get('state')

                            # Only append if both are valid
                            if hesitantPatient_status is not None and state is not None:
                                data.append({'STATE': state, 'ANTIVAX_STATUS': hesitantPatient_status})

    # 2. Create DataFrame
    df = pd.DataFrame(data)
    if df.empty:
        # Return empty data if no valid records
        return df, pd.DataFrame()

    # Normalize columns
    df.columns = df.columns.str.strip()

    # Convert to boolean
    df['ANTIVAX_STATUS'] = df['ANTIVAX_STATUS'].astype(bool)

    # 3. Group by state
    state_counts = (
        df.groupby('STATE', as_index=False)
        .agg(
            total_people=('ANTIVAX_STATUS', 'size'),
            hesitantPatient_count=('ANTIVAX_STATUS', 'sum')
        )
    )

    # 4. Calculate percentage
    state_counts['hesitantPatient_percentage'] = (
        state_counts['hesitantPatient_count'] / state_counts['total_people'] * 100
    ).fillna(0)

    # 5. Ensure all US states are present
    all_states = set([
        'AL', 'AK', 'AZ', 'AR', 'CA', 'CO', 'CT', 'DE', 'FL', 'GA', 'HI', 'ID', 'IL',
        'IN', 'IA', 'KS', 'KY', 'LA', 'ME', 'MD', 'MA', 'MI', 'MN', 'MS', 'MO', 'MT',
        'NE', 'NV', 'NH', 'NJ', 'NM', 'NY', 'NC', 'ND', 'OH', 'OK', 'OR', 'PA', 'RI',
        'SC', 'SD', 'TN', 'TX', 'UT', 'VT', 'VA', 'WA', 'WV', 'WI', 'WY'
    ])
    existing_states = set(state_counts['STATE'])
    missing_states = all_states - existing_states
    missing_states_df = pd.DataFrame({
        'STATE': list(missing_states),
        'total_people': 0,
        'hesitantPatient_count': 0,
        'hesitantPatient_percentage': 0
    })
    state_counts = pd.concat([state_counts, missing_states_df], ignore_index=True)

    return df, state_counts


def generate_choropleth_map(state_counts):
    """
    Use Plotly to generate a choropleth map visualizing the percentage
    of hesitant individuals by state, returning the figure object.
    """
    fig = px.choropleth(
        state_counts,
        locations='STATE',
        locationmode='USA-states',
        color='hesitantPatient_percentage',
        color_continuous_scale='Blues',
        scope='usa',
        title='HesitantPatient Individuals by State (Generated Population)',
        labels={
            'hesitantPatient_percentage': 'HesitantPatient Percentage',
            'total_people': 'Total People',
            'hesitantPatient_count': 'HesitantPatient Count'
        },
        hover_data={
            'total_people': ':,',
            'hesitantPatient_count': ':,',
            'hesitantPatient_percentage': ':.2f'
        }
    )

    # Enhance map visuals
    fig.update_geos(
        showcountries=False,
        showsubunits=True,
        subunitcolor="black",
        subunitwidth=1,
        showlakes=True,
        lakecolor="lightblue"
    )

    # Add state-specific labels to the map using geographic centroids for precise placement.
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

    for _, row in state_counts.iterrows():
        state = row['STATE']
        if state in state_centroids:
            hover_text = (
                f"State: {state}<br>"
                f"HesitantPatient Count: {row['hesitantPatient_count']:,}<br>"
                f"Total People: {row['total_people']:,}<br>"
                f"HesitantPatient Percentage: {row['hesitantPatient_percentage']:.2f}%"
            )
            fig.add_trace(go.Scattergeo(
                lat=[state_centroids[state]['lat']],
                lon=[state_centroids[state]['lon']],
                text=f"{row['hesitantPatient_count']}/{row['total_people']}",
                hovertext=hover_text,
                mode='text',
                textfont=dict(
                    size=10,
                    color='black' if row['hesitantPatient_count'] > 0 else 'gray'
                ),
                hoverinfo='text',
                showlegend=False
            ))

    # Summarize totals for the title
    total_people = state_counts['total_people'].sum()
    total_hesitantPatient = state_counts['hesitantPatient_count'].sum()
    hesitant_percentage = (
        (total_hesitantPatient / total_people) * 100 if total_people else 0
    )

    # Customize layout
    fig.update_layout(
        title=(
            f"<b>HesitantPatient Individuals by State (Generated Population)</b><br>"
            f"Total People: {total_people:,} | "
            f"Total HesitantPatient: {total_hesitantPatient:,} "
            f"({hesitant_percentage:.2f}%)"
        ),
        geo=dict(showocean=True, oceancolor='lightblue'),
        margin=dict(l=50, r=50, t=50, b=50),
        coloraxis_colorbar=dict(
            title='% HesitantPatient Individuals',
            tickvals=[0, 25, 50, 75, 100],
            ticktext=['0%', '25%', '50%', '75%', '100%']
        )
    )

    return fig


if __name__ == "__main__":
    # Example usage:
    json_folder_path = 'output/fhir/'
    df, state_counts = process_patient_data(json_folder_path)
    print(df.head())

    if not state_counts.empty:
        fig = generate_choropleth_map(state_counts)
        fig.show()
    else:
        print("No data found to visualize.")
