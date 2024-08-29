from datetime import datetime

def tremor_notification_message(patient_name, doctor_name, tremor_time=datetime.now(), tremor_severity="Moderate", notes="The patient reported mild panick post-incident."):
    html = f"""
    <html>
    <head>
        <style>
            body {{
                font-family: Arial, sans-serif;
                margin: 0;
                padding: 0;
                background-color: #f4f4f4;
            }}
            .container {{
                width: 100%;
                padding: 20px;
                background-color: #ffffff;
                box-shadow: 0 0 10px rgba(0, 0, 0, 0.1);
            }}
            .header {{
                background-color: #4CAF50;
                color: white;
                padding: 10px 0;
                text-align: center;
            }}
            .content {{
                padding: 20px;
            }}
            h2 {{
                color: #333333;
            }}
            p {{
                font-size: 16px;
                line-height: 1.6;
                color: #666666;
            }}
            .tremor-details {{
                margin-top: 20px;
            }}
            .tremor-details table {{
                width: 100%;
                border-collapse: collapse;
                margin-top: 10px;
            }}
            .tremor-details th, .tremor-details td {{
                border: 1px solid #dddddd;
                text-align: left;
                padding: 8px;
            }}
            .tremor-details th {{
                background-color: #f2f2f2;
                color: #333333;
            }}
            .footer {{
                text-align: center;
                padding: 10px 0;
                margin-top: 20px;
                background-color: #4CAF50;
                color: white;
            }}
        </style>
    </head>
    <body>
        <div class="container">
            <div class="header">
                <h1>Tremor Notification</h1>
            </div>
            <div class="content">
                <p>Dear Dr. {doctor_name},</p>
                <p>We would like to inform you of a recent tremor incident experienced by your patient, <strong>{patient_name}</strong>.</p>
                <div class="tremor-details">
                    <h2>Tremor Details</h2>
                    <table>
                        <tr>
                            <th>Patient Name</th>
                            <td>{patient_name}</td>
                        </tr>
                        <tr>
                            <th>Incident Time</th>
                            <td>{tremor_time}</td>
                        </tr>
                        <tr>
                            <th>Severity</th>
                            <td>{tremor_severity}</td>
                        </tr>
                        {f"""
                        <tr>
                            <th>Notes</th>
                            <td>{notes}</td>
                        </tr>
                        """ if notes else ""}
                    </table>
                </div>
                <p>Please review the details above and take any necessary actions. If you need further information, do not hesitate to contact us.</p>
                <p>Best regards,<br>BipolarBuddy Team</p>
            </div>
            <div class="footer">
                <p>Copyright &copy; {datetime.now().year} BipolarBuddy. All rights reserved.</p>
            </div>
        </div>
    </body>
    </html>
    """
    return html
