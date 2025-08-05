import 'package:flutter/material.dart';
import 'event_detail_screen.dart';

class EventListScreen extends StatefulWidget {
  const EventListScreen({super.key});

  @override
  State<EventListScreen> createState() => _EventListScreenState();
}

class _EventListScreenState extends State<EventListScreen> {
  String searchQuery = '';

  // Sample events for now
  final List<Map<String, dynamic>> events = [
    {
      'name': 'Speed Training',
      'category': 'Training',
      'choices': ['Focus on form', 'Push harder', 'Take it easy'],
      'effects': [
        'Speed +12, Technique +3',
        'Speed +20, Energy -15',
        'Speed +5, Energy +10',
      ],
    },
    {
      'name': 'Study Session',
      'category': 'Study',
      'choices': [
        'Study racing theory',
        'Practice with friends',
        'Rest instead',
      ],
      'effects': ['Intelligence +15', 'Intelligence +8, Mood +5', 'Energy +20'],
    },
    {
      'name': 'Morning Jog',
      'category': 'Training',
      'choices': ['Run at steady pace', 'Sprint intervals', 'Light walk'],
      'effects': [
        'Stamina +10, Speed +5',
        'Speed +15, Energy -10',
        'Energy +15',
      ],
    },
    {
      'name': 'Skill Practice',
      'category': 'Training',
      'choices': [
        'Practice new technique',
        'Perfect current skills',
        'Watch others',
      ],
      'effects': ['Technique +12', 'Power +8, Technique +5', 'Intelligence +5'],
    },
  ];

  List<Map<String, dynamic>> get filteredEvents {
    if (searchQuery.isEmpty) return events;
    return events
        .where(
          (event) =>
              event['name'].toLowerCase().contains(searchQuery.toLowerCase()),
        )
        .toList();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Training Events'),
        backgroundColor: Colors.blue,
      ),
      body: Column(
        children: [
          // Search Bar
          Padding(
            padding: const EdgeInsets.all(16.0),
            child: TextField(
              onChanged: (value) {
                setState(() {
                  searchQuery = value;
                });
              },
              decoration: const InputDecoration(
                hintText: 'Search events...',
                prefixIcon: Icon(Icons.search),
                border: OutlineInputBorder(),
              ),
            ),
          ),

          // Event List
          Expanded(
            child: ListView.builder(
              itemCount: filteredEvents.length,
              itemBuilder: (context, index) {
                final event = filteredEvents[index];
                return Card(
                  margin: const EdgeInsets.symmetric(
                    horizontal: 16,
                    vertical: 4,
                  ),
                  child: ListTile(
                    leading: CircleAvatar(
                      backgroundColor: Colors.blue,
                      child: Text(
                        event['category'][0],
                        style: const TextStyle(color: Colors.white),
                      ),
                    ),
                    title: Text(
                      event['name'],
                      style: const TextStyle(fontWeight: FontWeight.bold),
                    ),
                    subtitle: Text('Category: ${event['category']}'),
                    trailing: const Icon(Icons.arrow_forward_ios),
                    onTap: () {
                      Navigator.push(
                        context,
                        MaterialPageRoute(
                          builder: (context) => EventDetailScreen(event: event),
                        ),
                      );
                    },
                  ),
                );
              },
            ),
          ),
        ],
      ),
    );
  }
}
