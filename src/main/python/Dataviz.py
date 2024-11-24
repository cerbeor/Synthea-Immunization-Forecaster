import pandas as pd
import plotly.express as px

# Load your data
df = pd.read_csv('output/csv/patients.csv') 

# Ensure ANTIVAX_STATUS is a boolean
df['ANTIVAX_STATUS'] = df['ANTIVAX_STATUS'].astype(bool)

# Create a choropleth map showing the count of antivax individuals by state
antivax_counts = df[df['ANTIVAX_STATUS']].groupby('STATE').size().reset_index(name='count')

fig = px.choropleth(
    antivax_counts,
    locations='STATE',
    locationmode='USA-states',
    color='count',
    scope='usa',
    title='Number of Antivax Individuals by State'
)

fig.show()
