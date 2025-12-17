import json
import os
import pytest
import pandas as pd
from src.main.python.Dataviz import process_patient_data


@pytest.fixture
def setup_test_folder(tmp_path):
    """
    A pytest fixture that creates a temporary folder with some mock JSON files
    for testing. The tmp_path fixture provides a unique temporary directory.
    """

    # Create a JSON file with one patient who is 'hesitantPatient' = True in CA
    data_ca = {
        "entry": [
            {
                "resource": {
                    "resourceType": "Patient",
                    "extension": [
                        {
                            "url": "http://synthetichealth.github.io/synthea/hesitantPatient",
                            "valueBoolean": True
                        }
                    ],
                    "address": [
                        {"state": "CA"}
                    ]
                }
            }
        ]
    }

    # Create a JSON file with one patient who is 'hesitantPatient' = False in TX
    data_tx = {
        "entry": [
            {
                "resource": {
                    "resourceType": "Patient",
                    "extension": [
                        {
                            "url": "http://synthetichealth.github.io/synthea/hesitantPatient",
                            "valueBoolean": False
                        }
                    ],
                    "address": [
                        {"state": "TX"}
                    ]
                }
            }
        ]
    }

    # Write these JSON files into the temporary directory
    ca_file = tmp_path / "patient_ca.json"
    tx_file = tmp_path / "patient_tx.json"

    with open(ca_file, 'w') as f:
        json.dump(data_ca, f)
    with open(tx_file, 'w') as f:
        json.dump(data_tx, f)

    return tmp_path  # Return the path to the temporary folder

def test_process_patient_data(setup_test_folder):
    """
    Test that 'process_patient_data' correctly reads data from JSON files,
    builds the DataFrame, and groups by state.
    """
    # Use the fixture that sets up a folder of test JSON files
    test_folder = setup_test_folder

    # Call the function we want to test
    df, state_counts = process_patient_data(str(test_folder))

    # Check that the returned df is not empty
    assert not df.empty, "The DataFrame should not be empty."

    # We expect exactly 2 rows total from our fixture data
    assert len(df) == 2, "There should be 2 rows in the data (CA and TX)."

    # Check individual columns
    assert 'STATE' in df.columns, "DataFrame should have a 'STATE' column."
    assert 'HESITANT_PATIENT_STATUS' in df.columns, "DataFrame should have an 'HESITANT_PATIENT_STATUS' column."

    # Check that both states are in the grouped DataFrame
    expected_states_in_data = {'CA', 'TX'}
    actual_states_in_data = set(df['STATE'].unique())
    assert expected_states_in_data.issubset(actual_states_in_data), \
        "The DataFrame should contain CA and TX from our test data."

    # Check the grouping
    ca_row = state_counts[state_counts['STATE'] == 'CA'].iloc[0]
    tx_row = state_counts[state_counts['STATE'] == 'TX'].iloc[0]

    # For CA, total_people = 1, hesitantPatient_count = 1
    assert ca_row['total_people'] == 1
    assert ca_row['hesitantPatient_count'] == 1
    assert ca_row['hesitantPatient_percentage'] == 100.0

    # For TX, total_people = 1, hesitantPatient_count = 0
    assert tx_row['total_people'] == 1
    assert tx_row['hesitantPatient_count'] == 0
    assert tx_row['hesitantPatient_percentage'] == 0.0

    # Ensure the states for which we didn't provide data are also present but have zeros
    # (this verifies the code that adds missing states)
    all_us_states = [
        'AL', 'AK', 'AZ', 'AR', 'CA', 'CO', 'CT', 'DE', 'FL', 'GA',
        'HI', 'ID', 'IL', 'IN', 'IA', 'KS', 'KY', 'LA', 'ME', 'MD',
        'MA', 'MI', 'MN', 'MS', 'MO', 'MT', 'NE', 'NV', 'NH', 'NJ',
        'NM', 'NY', 'NC', 'ND', 'OH', 'OK', 'OR', 'PA', 'RI', 'SC',
        'SD', 'TN', 'TX', 'UT', 'VT', 'VA', 'WA', 'WV', 'WI', 'WY'
    ]
    for st in all_us_states:
        # Each state should appear in state_counts exactly once
        assert len(state_counts[state_counts['STATE'] == st]) == 1, f"{st} should appear in state_counts."

    print("Test passed: in Dataviz, process_patient_data() behaves as expected.")
