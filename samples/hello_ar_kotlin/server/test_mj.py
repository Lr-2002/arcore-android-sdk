from mujoco_ar import MujocoARConnector

# Initialize the connector with your desired parameters
connector = MujocoARConnector()

# Start the connector
connector.start()

# Retrieve the latest AR data (after connecting the iOS device, see the guide below)
while True:
    data = connector.get_latest_data()  # Returns {"position": (3, 1), "rotation": (3, 3), "button": bool, "toggle": bool}
    print(data)